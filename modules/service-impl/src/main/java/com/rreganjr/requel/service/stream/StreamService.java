/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
    private final StreamTargetAccess targetAccess;
    private final ConcurrentHashMap<String, EmitterHolder> localEmitters = new ConcurrentHashMap<>();

    /**
     * #450: serializes the session lifecycle: claiming a session and swapping in its emitter
     * ({@link #createStream}), and checking-and-removing it when an emitter ends
     * ({@link #onEmitterDone}). Without it a reconnect and the old emitter's completion could
     * interleave and leave a live emitter whose session the store had already forgotten. Only
     * connects and disconnects take it; event delivery doesn't.
     */
    private final Object lifecycleLock = new Object();

    public StreamService(StreamSessionStore sessionStore,
                         @Qualifier("streamTaskScheduler") TaskScheduler taskScheduler,
                         ObjectMapper objectMapper, MeterRegistry meterRegistry,
                         StreamTargetAccess targetAccess) {
        this.sessionStore = sessionStore;
        this.taskScheduler = taskScheduler;
        this.objectMapper = objectMapper;
        this.targetAccess = targetAccess;

        Gauge.builder("requel.sse.active_connections", localEmitters, ConcurrentHashMap::size)
                .description("Active SSE connections")
                .register(meterRegistry);
    }

    /**
     * Open a stream: a new session, or a reconnect to the caller's own session.
     * <p>
     * #450: {@code requestedSessionId} is the client's previous session id, if any.
     * <ul>
     *   <li>Another user's session: 403, and nothing about that session changes.</li>
     *   <li>The caller's own session: the new emitter replaces the old one, whose keep-alive and
     *       expiry tasks are cancelled and whose emitter is completed; the subscriptions stay.</li>
     *   <li>An unknown id (expired, or the server restarted) or none: a new session with a
     *       server-generated id, which the client learns from the {@code Session} event.</li>
     * </ul>
     * Initial subscriptions the caller may not read are skipped (and logged) rather than failing
     * the stream; the client replays its subscriptions on every reconnect, and one lost project
     * mustn't break the rest.
     */
    public SseEmitter createStream(String requestedSessionId, String ownerUsername,
                                    long jwtExpiresAtEpochMs,
                                    List<String> initialSubscriptionKeys) {
        // Access checks read the database: done before taking the lifecycle lock.
        List<String> allowedKeys = new ArrayList<>();
        for (String key : initialSubscriptionKeys) {
            if (targetAccess.canSubscribe(ownerUsername, key)) {
                allowedKeys.add(key);
            } else {
                log.info("createStream owner={} skipped subscription {}: not readable", ownerUsername, key);
            }
        }

        SseEmitter emitter = new SseEmitter(-1L);
        String sessionId;
        EmitterHolder holder;
        EmitterHolder replaced;
        synchronized (lifecycleLock) {
            sessionId = sessionStore.claimSession(requestedSessionId, ownerUsername);
            if (sessionId == null) {
                log.warn("createStream refused: session {} does not belong to {}",
                        requestedSessionId, ownerUsername);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Session does not belong to the authenticated user");
            }
            String scheduledFor = sessionId;
            ScheduledFuture<?> keepAlive = taskScheduler.scheduleAtFixedRate(
                    () -> sendKeepAlive(scheduledFor),
                    Instant.now().plusMillis(KEEP_ALIVE_INTERVAL_MS),
                    java.time.Duration.ofMillis(KEEP_ALIVE_INTERVAL_MS));
            ScheduledFuture<?> expiryFuture = jwtExpiresAtEpochMs > 0
                    ? taskScheduler.schedule(
                        () -> sendSessionExpiredAndClose(scheduledFor),
                        Instant.ofEpochMilli(jwtExpiresAtEpochMs + SESSION_EXPIRY_GRACE_MS))
                    : null;
            holder = new EmitterHolder(emitter, keepAlive, expiryFuture);
            replaced = localEmitters.put(sessionId, holder);
            // Register initial subscriptions BEFORE sending the Session event.
            // Reasoning: emitter.send() inside sendEvent may commit the HTTP
            // response (200 OK + first chunk) — at that point the client's
            // waitForResponse for /events/stream resolves and the test may
            // proceed to fire commands. Registering subscriptions first
            // closes a (small but real) race window where a Project broadcast
            // emitted between sendEvent and addSubscription would find no
            // subscribers and be silently dropped.
            for (String key : allowedKeys) {
                sessionStore.addSubscription(sessionId, key);
            }
        }
        if (replaced != null) {
            // #450: the replaced emitter's tasks used to run for the life of the server, sending
            // keep-alives to whichever emitter held the id. Its completion callback finds it is no
            // longer the current holder and leaves the session alone.
            replaced.cancel();
            try {
                replaced.emitter().complete();
            } catch (RuntimeException alreadyDone) {
                log.debug("completing replaced emitter for session {}: {}", sessionId,
                        alreadyDone.getMessage());
            }
        }

        log.debug("createStream session={} owner={} initialSubs={} (requestedSessionId={}, reconnect={})",
                sessionId, ownerUsername, allowedKeys, requestedSessionId, replaced != null);

        // Send Session event
        sendEvent(sessionId, StreamEventEnvelope.session(sessionId));

        // Cleanup callbacks
        String callbackSession = sessionId;
        EmitterHolder callbackHolder = holder;
        emitter.onCompletion(() -> onEmitterDone(callbackSession, callbackHolder));
        emitter.onTimeout(() -> onEmitterDone(callbackSession, callbackHolder));
        emitter.onError(e -> onEmitterDone(callbackSession, callbackHolder));

        return emitter;
    }

    /**
     * #450: refuse (403) a subscription the caller may not read. The same answer for an unknown
     * type, a missing entity and a project the caller isn't on, so nothing is learned about what
     * exists.
     */
    public void verifyCanSubscribe(String username, String targetKey) {
        if (!targetAccess.canSubscribe(username, targetKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Not allowed to subscribe to " + targetKey);
        }
    }

    /**
     * Verify that the caller owns the given session. Throws 403 if not.
     * Unknown sessions are rejected — either the session expired or the ID is wrong.
     */
    public void verifyOwnership(String sessionId, String callerUsername) {
        String owner = sessionStore.getSessionOwner(sessionId);
        if (owner == null || !owner.equals(callerUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Session does not belong to the authenticated user");
        }
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
     *
     * Resilience contract: a failure to deliver to one session MUST NOT prevent
     * delivery to the others. Stale sessions (browsers that closed without a
     * graceful disconnect) accumulate in the session store until {@code onError}
     * / {@code onCompletion} fire — that callback may not fire promptly on a
     * hard-aborted connection, so a broken-pipe write here is the first signal
     * we get that a session is dead. We catch every throwable from {@link #sendEvent}
     * per-session and prune the offender so future broadcasts skip it.
     */
    public void pushToSubscribedSessions(String targetType, Long targetId, Object payload) {
        pushToSubscribedSessions(targetType, targetId, payload, null);
    }

    /**
     * Same as {@link #pushToSubscribedSessions(String, Long, Object)} but skips
     * {@code excludeSessionId} (typically the session that issued the command, so it is not told to
     * refresh an entity it just changed). A null {@code excludeSessionId} excludes nobody. The
     * Project:0 broadcast never passes an exclusion — the acting session's sidebar counts must still
     * update — so only targeted events are filtered.
     */
    public void pushToSubscribedSessions(String targetType, Long targetId, Object payload,
                                         String excludeSessionId) {
        String targetKey = targetType + ":" + targetId;
        Set<String> sessionIds = sessionStore.getSessionsForTarget(targetKey);
        log.debug("pushToSubscribedSessions targetKey={} sessions={} exclude={}", targetKey,
                sessionIds.size(), excludeSessionId);
        StreamEventEnvelope envelope = payload != null
                ? StreamEventEnvelope.data(targetType, targetId, payload)
                : StreamEventEnvelope.targetDeleted(targetType, targetId);
        for (String sessionId : sessionIds) {
            if (excludeSessionId != null && excludeSessionId.equals(sessionId)) {
                log.debug("pushToSubscribedSessions skipping originator session {} for target {}",
                        sessionId, targetKey);
                continue;
            }
            try {
                sendEvent(sessionId, envelope);
            } catch (RuntimeException broadcastFailure) {
                // sendEvent already logs at WARN with diagnostic detail and
                // attempts to clean up the dead session. Swallow here so the
                // remaining recipients still get the event. Anything that
                // bubbles past sendEvent's own catches is a genuine bug, not
                // a per-session-delivery failure — log it loud and keep going.
                log.warn("Unexpected error broadcasting to session {} for target {}: {}",
                        sessionId, targetKey, broadcastFailure.getMessage(), broadcastFailure);
            }
        }
    }

    private void sendEvent(String sessionId, StreamEventEnvelope envelope) {
        EmitterHolder holder = localEmitters.get(sessionId);
        if (holder == null) {
            // Session was already pruned (e.g. by a previous broken-pipe in this
            // same broadcast) but the targetToSessions index is eventually-consistent.
            // Make sure the index also forgets it so we don't keep iterating it.
            // #450: re-checked under the lifecycle lock, so a session a reconnect is
            // re-attaching right now isn't removed from under it.
            synchronized (lifecycleLock) {
                if (localEmitters.get(sessionId) == null) {
                    sessionStore.removeSession(sessionId);
                }
            }
            log.debug("sendEvent session={} eventType={} → DROPPED (no emitter, pruned from index)",
                    sessionId, envelope.eventType());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(envelope);
            holder.emitter().send(SseEmitter.event().data(json));
            log.debug("sendEvent session={} eventType={} target={}:{} → flushed {} bytes",
                    sessionId, envelope.eventType(), envelope.targetType(), envelope.targetId(),
                    json.length());
        } catch (IOException | IllegalStateException e) {
            // IOException: typically "Broken pipe" — the client disconnected without
            //   a graceful close so onCompletion/onError haven't fired yet.
            // IllegalStateException: Tomcat throws this when a background thread
            //   (e.g. NLP taskExecutor, or a broadcast triggered by a previous
            //   send error) tries to use the AsyncContext after onError already
            //   ran. completeWithError(e) below ALSO throws IllegalStateException
            //   in that case — we catch both varieties here.
            // Either way the session is dead. Best-effort completeWithError to
            // tell Spring; if that itself throws, swallow (the session is going
            // away regardless). Then prune the holder so subsequent broadcasts
            // skip it instead of repeating the failure.
            log.warn("sendEvent session={} target={}:{} → {}: {} — pruning dead session",
                    sessionId, envelope.targetType(), envelope.targetId(),
                    e.getClass().getSimpleName(), e.getMessage());
            try {
                holder.emitter().completeWithError(e);
            } catch (RuntimeException completeFailure) {
                log.debug("completeWithError on session {} also failed: {}",
                        sessionId, completeFailure.getMessage());
            }
            onEmitterDone(sessionId, holder);
        }
    }

    private void sendKeepAlive(String sessionId) {
        EmitterHolder holder = localEmitters.get(sessionId);
        if (holder == null) return;
        try {
            holder.emitter().send(SseEmitter.event().comment("keep-alive"));
        } catch (IOException | IllegalStateException e) {
            // Same dead-session signal as in sendEvent (see comment there). For a
            // session that never receives broadcasts, the keep-alive is the only
            // signal that the connection died — without proactive pruning here
            // the session would linger in the store forever (the cause of the
            // stale-session accumulation we saw in coverage/web.log).
            log.debug("Keep-alive failed for session {}: {} — pruning dead session",
                    sessionId, e.getMessage());
            try {
                holder.emitter().completeWithError(e);
            } catch (RuntimeException ignored) {
                // already-completed emitter; nothing more to do
            }
            onEmitterDone(sessionId, holder);
        }
    }

    private void sendSessionExpiredAndClose(String sessionId) {
        sendEvent(sessionId, StreamEventEnvelope.sessionExpired());
        closeConnection(sessionId);
    }

    private void onEmitterDone(String sessionId, EmitterHolder holder) {
        // Stale holder guard: only remove the session if this is still the active holder. A holder
        // a reconnect replaced must not take the reconnected session with it (#450).
        synchronized (lifecycleLock) {
            if (localEmitters.get(sessionId) == holder) {
                localEmitters.remove(sessionId, holder);
                sessionStore.removeSession(sessionId);
            }
        }
        holder.cancel();
    }

    private record EmitterHolder(SseEmitter emitter, ScheduledFuture<?> keepAlive,
                                  ScheduledFuture<?> expiryFuture) {
        void cancel() {
            if (keepAlive != null) keepAlive.cancel(false);
            if (expiryFuture != null) expiryFuture.cancel(false);
        }
    }
}
