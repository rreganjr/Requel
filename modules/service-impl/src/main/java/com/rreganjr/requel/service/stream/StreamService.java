package com.rreganjr.requel.service.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages SSE stream sessions, SseEmitter lifecycle, keep-alive, and session expiry.
 */
@Service
public class StreamService {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);
    private static final long KEEP_ALIVE_INTERVAL_MS = 30_000L;
    private static final long SESSION_EXPIRY_GRACE_MS = 5 * 60_000L;

    private final StreamSessionStore sessionStore;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, EmitterHolder> localEmitters = new ConcurrentHashMap<>();

    public StreamService(StreamSessionStore sessionStore,
                         @Qualifier("streamTaskScheduler") TaskScheduler taskScheduler,
                         ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.sessionStore = sessionStore;
        this.taskScheduler = taskScheduler;
        this.objectMapper = objectMapper;

        Gauge.builder("requel.sse.active_connections", localEmitters, ConcurrentHashMap::size)
                .description("Active SSE connections")
                .register(meterRegistry);
    }

    /**
     * Create a new stream session or reattach to an existing one.
     */
    public SseEmitter createStream(String existingSessionId, long jwtExpiresAtEpochMs,
                                    List<String> initialSubscriptionKeys) {
        String sessionId = existingSessionId != null ? existingSessionId : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(-1L);

        sessionStore.createSession(sessionId);

        ScheduledFuture<?> keepAlive = taskScheduler.scheduleAtFixedRate(
                () -> sendKeepAlive(sessionId),
                Instant.now().plusMillis(KEEP_ALIVE_INTERVAL_MS),
                java.time.Duration.ofMillis(KEEP_ALIVE_INTERVAL_MS));

        ScheduledFuture<?> expiryFuture = jwtExpiresAtEpochMs > 0
                ? taskScheduler.schedule(
                    () -> sendSessionExpiredAndClose(sessionId),
                    Instant.ofEpochMilli(jwtExpiresAtEpochMs + SESSION_EXPIRY_GRACE_MS))
                : null;

        EmitterHolder holder = new EmitterHolder(emitter, keepAlive, expiryFuture);
        localEmitters.put(sessionId, holder);

        // Send Session event
        sendEvent(sessionId, StreamEventEnvelope.session(sessionId));

        // Register initial subscriptions
        for (String key : initialSubscriptionKeys) {
            sessionStore.addSubscription(sessionId, key);
        }

        // Cleanup callbacks
        emitter.onCompletion(() -> onEmitterDone(sessionId, holder));
        emitter.onTimeout(() -> onEmitterDone(sessionId, holder));
        emitter.onError(e -> onEmitterDone(sessionId, holder));

        return emitter;
    }

    public void addSubscription(String sessionId, String targetKey) {
        sessionStore.addSubscription(sessionId, targetKey);
    }

    public void removeSubscription(String sessionId, String targetKey) {
        sessionStore.removeSubscription(sessionId, targetKey);
    }

    /**
     * Graceful server-side disconnect: complete the emitter so the browser
     * gets a clean end-of-stream.
     */
    public void closeConnection(String sessionId) {
        EmitterHolder holder = localEmitters.get(sessionId);
        if (holder != null) {
            holder.emitter().complete();
        }
    }

    /**
     * Push an event to all sessions subscribed to the given target.
     */
    public void pushToSubscribedSessions(String targetType, Long targetId, Object payload) {
        String targetKey = targetType + ":" + targetId;
        Set<String> sessionIds = sessionStore.getSessionsForTarget(targetKey);
        StreamEventEnvelope envelope = payload != null
                ? StreamEventEnvelope.data(targetType, targetId, payload)
                : StreamEventEnvelope.targetDeleted(targetType, targetId);
        for (String sessionId : sessionIds) {
            sendEvent(sessionId, envelope);
        }
    }

    private void sendEvent(String sessionId, StreamEventEnvelope envelope) {
        EmitterHolder holder = localEmitters.get(sessionId);
        if (holder == null) return;
        try {
            String json = objectMapper.writeValueAsString(envelope);
            holder.emitter().send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.debug("Failed to send event to session {}: {}", sessionId, e.getMessage());
            holder.emitter().completeWithError(e);
        }
    }

    private void sendKeepAlive(String sessionId) {
        EmitterHolder holder = localEmitters.get(sessionId);
        if (holder == null) return;
        try {
            holder.emitter().send(SseEmitter.event().comment("keep-alive"));
        } catch (IOException e) {
            log.debug("Keep-alive failed for session {}", sessionId);
        }
    }

    private void sendSessionExpiredAndClose(String sessionId) {
        sendEvent(sessionId, StreamEventEnvelope.sessionExpired());
        closeConnection(sessionId);
    }

    private void onEmitterDone(String sessionId, EmitterHolder holder) {
        // Stale holder guard: only clean up if this is still the active holder
        if (localEmitters.get(sessionId) == holder) {
            localEmitters.remove(sessionId, holder);
            sessionStore.removeSession(sessionId);
            holder.cancel();
        }
    }

    public String getSessionId() {
        return null; // placeholder for session lookup
    }

    private record EmitterHolder(SseEmitter emitter, ScheduledFuture<?> keepAlive,
                                  ScheduledFuture<?> expiryFuture) {
        void cancel() {
            if (keepAlive != null) keepAlive.cancel(false);
            if (expiryFuture != null) expiryFuture.cancel(false);
        }
    }
}
