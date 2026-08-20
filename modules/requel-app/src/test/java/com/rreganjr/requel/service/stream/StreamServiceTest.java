/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StreamService}.
 *
 * Uses a real {@link StreamSessionStore} (already unit-tested separately) and
 * a real {@link ObjectMapper} so the session-event JSON emitted during
 * {@code createStream} serialises correctly. {@link TaskScheduler} is mocked
 * to prevent actual timer threads from starting. {@link SimpleMeterRegistry}
 * satisfies the Micrometer gauge registration without a full Spring context.
 *
 * Scenarios covered:
 * - verifyOwnership: passes when caller is the session owner
 * - verifyOwnership: throws 403 when caller is not the owner
 * - verifyOwnership: throws 403 for an unknown session (owner is null)
 * - createStream with null existingSessionId: new session registered in store
 * - createStream with an existing session ID: that ID is reused
 * - createStream with initial subscriptions: keys registered in store
 * - createStream returns a non-null SseEmitter
 * - addSubscription delegates to the session store
 * - removeSubscription delegates to the session store
 * - closeConnection via onCompletion callback: triggers session cleanup in store
 */
class StreamServiceTest {

    private StreamSessionStore sessionStore;
    private TaskScheduler taskScheduler;
    private StreamService streamService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        sessionStore = new StreamSessionStore();
        taskScheduler = mock(TaskScheduler.class);

        // Both schedule methods must return a ScheduledFuture to avoid NPE
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(), any());
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));

        streamService = new StreamService(
                sessionStore, taskScheduler, new ObjectMapper(), new SimpleMeterRegistry());
    }

    // -------------------------------------------------------------------------
    // verifyOwnership
    // -------------------------------------------------------------------------

    @Test
    void verifyOwnershipPassesWhenCallerIsOwner() {
        streamService.createStream("sess-1", "alice", 0L, List.of());

        // Should not throw
        assertThatNoException().isThrownBy(
                () -> streamService.verifyOwnership("sess-1", "alice"));
    }

    @Test
    void verifyOwnershipThrows403WhenCallerIsNotOwner() {
        streamService.createStream("sess-1", "alice", 0L, List.of());

        assertThatThrownBy(() -> streamService.verifyOwnership("sess-1", "bob"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void verifyOwnershipThrows403ForUnknownSession() {
        assertThatThrownBy(() -> streamService.verifyOwnership("no-such-session", "alice"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // -------------------------------------------------------------------------
    // createStream
    // -------------------------------------------------------------------------

    @Test
    void createStreamReturnsNonNullEmitter() {
        SseEmitter emitter = streamService.createStream(null, "alice", 0L, List.of());
        assertThat(emitter).isNotNull();
    }

    @Test
    void createStreamWithNullIdRegistersNewSessionInStore() {
        streamService.createStream(null, "alice", 0L, List.of());

        // A new session was created — count should be 1
        assertThat(sessionStore.getActiveSessionCount()).isEqualTo(1);
    }

    @Test
    void createStreamWithExistingIdUsesthatId() {
        streamService.createStream("existing-id", "alice", 0L, List.of());

        assertThat(sessionStore.getSessionOwner("existing-id")).isEqualTo("alice");
    }

    @Test
    void createStreamRegistersInitialSubscriptions() {
        streamService.createStream("sess-1", "alice", 0L, List.of("Goal:1", "Project:2"));

        assertThat(sessionStore.getSubscriptions("sess-1"))
                .containsExactlyInAnyOrder("Goal:1", "Project:2");
    }

    @Test
    void createStreamSchedulesKeepAlive() {
        streamService.createStream(null, "alice", 0L, List.of());

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(), any());
    }

    @Test
    void createStreamSchedulesExpiryWhenJwtExpiryProvided() {
        streamService.createStream(null, "alice", System.currentTimeMillis() + 60_000L, List.of());

        verify(taskScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
    }

    @Test
    void createStreamDoesNotScheduleExpiryWhenJwtExpiryIsZero() {
        streamService.createStream(null, "alice", 0L, List.of());

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(java.time.Instant.class));
    }

    // -------------------------------------------------------------------------
    // addSubscription / removeSubscription — delegation
    // -------------------------------------------------------------------------

    @Test
    void addSubscriptionDelegatesToStore() {
        streamService.createStream("sess-1", "alice", 0L, List.of());
        streamService.addSubscription("sess-1", "Story:5");

        assertThat(sessionStore.getSubscriptions("sess-1")).contains("Story:5");
    }

    @Test
    void removeSubscriptionDelegatesToStore() {
        streamService.createStream("sess-1", "alice", 0L, List.of("Actor:3"));
        streamService.removeSubscription("sess-1", "Actor:3");

        assertThat(sessionStore.getSubscriptions("sess-1")).doesNotContain("Actor:3");
    }

    // -------------------------------------------------------------------------
    // closeConnection
    // -------------------------------------------------------------------------

    @Test
    void closeConnectionOnValidSessionDoesNotThrow() {
        streamService.createStream("sess-1", "alice", 0L, List.of());

        // Should not throw — completes the emitter held for this session
        assertThatNoException().isThrownBy(() -> streamService.closeConnection("sess-1"));
    }

    @Test
    void closeConnectionOnUnknownSessionIsNoOp() {
        // localEmitters.get returns null; the null-guard prevents any action
        assertThatNoException().isThrownBy(() -> streamService.closeConnection("no-such-session"));
    }

    // -------------------------------------------------------------------------
    // pushToSubscribedSessions — broadcast resilience
    //
    // Regression coverage for the bug surfaced in coverage/web.log on the
    // SSE refresh E2E: when one session's emitter throws (broken pipe from
    // a hard-aborted client), the original loop terminated and the remaining
    // subscribers never received the event. The fix adds per-session
    // try/catch and prunes the dead session so future broadcasts skip it.
    // -------------------------------------------------------------------------

    /**
     * Replace the live emitter behind {@code sessionId} with a mock so the test
     * can dictate what {@code emitter.send(...)} does. We use reflection because
     * {@code localEmitters} is intentionally private — exposing a setter just
     * for tests would weaken the encapsulation. Reflection is contained to this
     * helper so the brittleness lives in one place.
     */
    @SuppressWarnings("unchecked")
    private SseEmitter swapInMockEmitter(String sessionId) throws Exception {
        Field localEmittersField = StreamService.class.getDeclaredField("localEmitters");
        localEmittersField.setAccessible(true);
        Map<String, Object> localEmitters =
                (Map<String, Object>) localEmittersField.get(streamService);

        Object oldHolder = localEmitters.get(sessionId);
        assertThat(oldHolder).as("test must seed session via createStream first").isNotNull();

        // EmitterHolder is a private record — reflectively read its three components
        // and rebuild it with a mock emitter swapped in.
        Class<?> holderClass = oldHolder.getClass();
        Field emitterField = holderClass.getDeclaredField("emitter");
        Field keepAliveField = holderClass.getDeclaredField("keepAlive");
        Field expiryField = holderClass.getDeclaredField("expiryFuture");
        emitterField.setAccessible(true);
        keepAliveField.setAccessible(true);
        expiryField.setAccessible(true);

        SseEmitter mockEmitter = mock(SseEmitter.class);
        var holderCtor = holderClass.getDeclaredConstructors()[0];
        holderCtor.setAccessible(true);
        Object newHolder = holderCtor.newInstance(
                mockEmitter, keepAliveField.get(oldHolder), expiryField.get(oldHolder));
        localEmitters.put(sessionId, newHolder);
        return mockEmitter;
    }

    @Test
    void pushBroadcastDeliversToHealthySessionsEvenWhenOneSessionThrowsIOException() throws Exception {
        // Three sessions all subscribed to Project:0. Middle one's emitter
        // throws "Broken pipe" on send — represents a browser tab that was
        // closed without a graceful disconnect.
        streamService.createStream("alive-1", "alice", 0L, List.of("Project:0"));
        streamService.createStream("dead",    "alice", 0L, List.of("Project:0"));
        streamService.createStream("alive-2", "alice", 0L, List.of("Project:0"));

        SseEmitter alive1 = swapInMockEmitter("alive-1");
        SseEmitter dead   = swapInMockEmitter("dead");
        SseEmitter alive2 = swapInMockEmitter("alive-2");

        doThrow(new IOException("Broken pipe")).when(dead).send(any(SseEmitter.SseEventBuilder.class));

        // Must not propagate — single-session failure is contained.
        assertThatNoException().isThrownBy(
                () -> streamService.pushToSubscribedSessions("Project", 0L, Map.of("type", "refresh")));

        // Both healthy sessions received the event. The dead session was attempted
        // (so it threw) and then pruned via completeWithError + onEmitterDone.
        verify(alive1).send(any(SseEmitter.SseEventBuilder.class));
        verify(alive2).send(any(SseEmitter.SseEventBuilder.class));
        verify(dead).send(any(SseEmitter.SseEventBuilder.class));
        verify(dead).completeWithError(any(IOException.class));

        // Dead session removed from the index so the next broadcast won't keep
        // re-attempting it. Healthy sessions remain.
        assertThat(sessionStore.getSessionsForTarget("Project:0"))
                .containsExactlyInAnyOrder("alive-1", "alive-2");
    }

    @Test
    void pushBroadcastSurvivesIllegalStateExceptionFromCompleteWithError() throws Exception {
        // Reproduces the exact production trace: send() throws IOException, then
        // completeWithError(e) ALSO throws IllegalStateException because Tomcat
        // already fired AsyncListener.onError. Original code didn't catch the
        // second throw and aborted the loop.
        streamService.createStream("alive",  "alice", 0L, List.of("Project:0"));
        streamService.createStream("doomed", "alice", 0L, List.of("Project:0"));

        SseEmitter alive  = swapInMockEmitter("alive");
        SseEmitter doomed = swapInMockEmitter("doomed");

        doThrow(new IOException("Broken pipe")).when(doomed)
                .send(any(SseEmitter.SseEventBuilder.class));
        doThrow(new IllegalStateException(
                "A non-container (application) thread attempted to use the AsyncContext after an error had occurred"))
                .when(doomed).completeWithError(any());

        assertThatNoException().isThrownBy(
                () -> streamService.pushToSubscribedSessions("Project", 0L, Map.of("type", "refresh")));

        // Healthy session received the event despite the cascade failure.
        verify(alive).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void pushBroadcastIsNoOpWhenNoSessionsAreSubscribed() {
        streamService.createStream("sess-1", "alice", 0L, List.of("Goal:5"));

        // No subscribers for Project:0 — must not throw, must not visit any session
        assertThatNoException().isThrownBy(
                () -> streamService.pushToSubscribedSessions("Project", 0L, Map.of("type", "refresh")));
    }

    // -------------------------------------------------------------------------
    // pushToSubscribedSessions — originating-session exclusion (issue #178 §4.4)
    // -------------------------------------------------------------------------

    @Test
    void pushToSubscribedSessionsSkipsExcludedSession() throws Exception {
        streamService.createStream("originator", "alice", 0L, List.of("Goal:1"));
        streamService.createStream("other",      "alice", 0L, List.of("Goal:1"));

        SseEmitter originator = swapInMockEmitter("originator");
        SseEmitter other      = swapInMockEmitter("other");

        streamService.pushToSubscribedSessions("Goal", 1L, Map.of("type", "refresh"), "originator");

        // The originating session is skipped; every other subscriber still receives the event.
        verify(originator, never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(other).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void pushToSubscribedSessionsWithNullExclusionDeliversToAll() throws Exception {
        streamService.createStream("s1", "alice", 0L, List.of("Goal:1"));
        streamService.createStream("s2", "alice", 0L, List.of("Goal:1"));

        SseEmitter e1 = swapInMockEmitter("s1");
        SseEmitter e2 = swapInMockEmitter("s2");

        streamService.pushToSubscribedSessions("Goal", 1L, Map.of("type", "refresh"), null);

        verify(e1).send(any(SseEmitter.SseEventBuilder.class));
        verify(e2).send(any(SseEmitter.SseEventBuilder.class));
    }
}
