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
                sessionStore, taskScheduler, new ObjectMapper(), new SimpleMeterRegistry(),
                allowAll);
    }

    /** The access rule most tests run with; {@link #denied} is the restrictive one. */
    private final StreamTargetAccess allowAll = (user, key) -> true;

    /**
     * Open a stream under a known id. Since #450 a client can't choose a new session's id (an
     * unknown id gets a server-generated one), so the test registers the session first, as an
     * earlier stream would have, and the stream re-attaches to it.
     */
    private SseEmitter openSeeded(String sessionId, String owner, long jwtExpiry, List<String> subs) {
        sessionStore.createSession(sessionId, owner);
        return streamService.createStream(sessionId, owner, jwtExpiry, subs);
    }

    // -------------------------------------------------------------------------
    // verifyOwnership
    // -------------------------------------------------------------------------

    @Test
    void verifyOwnershipPassesWhenCallerIsOwner() {
        openSeeded("sess-1", "alice", 0L, List.of());

        // Should not throw
        assertThatNoException().isThrownBy(
                () -> streamService.verifyOwnership("sess-1", "alice"));
    }

    @Test
    void verifyOwnershipThrows403WhenCallerIsNotOwner() {
        openSeeded("sess-1", "alice", 0L, List.of());

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
    void createStreamWithTheOwnersExistingIdUsesThatId() {
        openSeeded("existing-id", "alice", 0L, List.of());

        assertThat(sessionStore.getSessionOwner("existing-id")).isEqualTo("alice");
        assertThat(sessionStore.getActiveSessionCount()).isEqualTo(1);
    }

    @Test
    void createStreamRegistersInitialSubscriptions() {
        openSeeded("sess-1", "alice", 0L, List.of("Goal:1", "Project:2"));

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
        openSeeded("sess-1", "alice", 0L, List.of());
        streamService.addSubscription("sess-1", "Story:5");

        assertThat(sessionStore.getSubscriptions("sess-1")).contains("Story:5");
    }

    @Test
    void removeSubscriptionDelegatesToStore() {
        openSeeded("sess-1", "alice", 0L, List.of("Actor:3"));
        streamService.removeSubscription("sess-1", "Actor:3");

        assertThat(sessionStore.getSubscriptions("sess-1")).doesNotContain("Actor:3");
    }

    // -------------------------------------------------------------------------
    // closeConnection
    // -------------------------------------------------------------------------

    @Test
    void closeConnectionOnValidSessionDoesNotThrow() {
        openSeeded("sess-1", "alice", 0L, List.of());

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
        openSeeded("alive-1", "alice", 0L, List.of("Project:0"));
        openSeeded("dead",    "alice", 0L, List.of("Project:0"));
        openSeeded("alive-2", "alice", 0L, List.of("Project:0"));

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
        openSeeded("alive",  "alice", 0L, List.of("Project:0"));
        openSeeded("doomed", "alice", 0L, List.of("Project:0"));

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
        openSeeded("sess-1", "alice", 0L, List.of("Goal:5"));

        // No subscribers for Project:0 — must not throw, must not visit any session
        assertThatNoException().isThrownBy(
                () -> streamService.pushToSubscribedSessions("Project", 0L, Map.of("type", "refresh")));
    }

    // -------------------------------------------------------------------------
    // pushToSubscribedSessions — originating-session exclusion (issue #178 §4.4)
    // -------------------------------------------------------------------------

    @Test
    void pushToSubscribedSessionsSkipsExcludedSession() throws Exception {
        openSeeded("originator", "alice", 0L, List.of("Goal:1"));
        openSeeded("other",      "alice", 0L, List.of("Goal:1"));

        SseEmitter originator = swapInMockEmitter("originator");
        SseEmitter other      = swapInMockEmitter("other");

        streamService.pushToSubscribedSessions("Goal", 1L, Map.of("type", "refresh"), "originator");

        // The originating session is skipped; every other subscriber still receives the event.
        verify(originator, never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(other).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void pushToSubscribedSessionsWithNullExclusionDeliversToAll() throws Exception {
        openSeeded("s1", "alice", 0L, List.of("Goal:1"));
        openSeeded("s2", "alice", 0L, List.of("Goal:1"));

        SseEmitter e1 = swapInMockEmitter("s1");
        SseEmitter e2 = swapInMockEmitter("s2");

        streamService.pushToSubscribedSessions("Goal", 1L, Map.of("type", "refresh"), null);

        verify(e1).send(any(SseEmitter.SseEventBuilder.class));
        verify(e2).send(any(SseEmitter.SseEventBuilder.class));
    }

    // -------------------------------------------------------------------------
    // #450: reconnect ownership, emitter replacement, subscription access
    // -------------------------------------------------------------------------

    /** Every scheduled task gets its own future, recorded in order, so tests can see which were cancelled. */
    private List<ScheduledFuture<?>> distinctFutures() {
        List<ScheduledFuture<?>> futures = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        org.mockito.stubbing.Answer<ScheduledFuture<?>> answer = inv -> {
            ScheduledFuture<?> f = mock(ScheduledFuture.class);
            futures.add(f);
            return f;
        };
        doAnswer(answer).when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(), any());
        doAnswer(answer).when(taskScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
        return futures;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> localEmitters() throws Exception {
        Field field = StreamService.class.getDeclaredField("localEmitters");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(streamService);
    }

    private static Object component(Object holder, String name) throws Exception {
        Field field = holder.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(holder);
    }

    @Test
    void anotherUsersReconnectIsRefusedAndChangesNothing() throws Exception {
        List<ScheduledFuture<?>> futures = distinctFutures();
        openSeeded("sess-1", "alice", 0L, List.of("Goal:1"));
        Object aliceHolder = localEmitters().get("sess-1");

        assertThatThrownBy(() -> streamService.createStream("sess-1", "bob", 0L, List.of("Goal:2")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(sessionStore.getSessionOwner("sess-1")).isEqualTo("alice");
        assertThat(sessionStore.getSubscriptions("sess-1")).containsExactly("Goal:1");
        assertThat(sessionStore.getSessionsForTarget("Goal:2")).isEmpty();
        assertThat(localEmitters().get("sess-1")).isSameAs(aliceHolder);
        assertThat(futures).hasSize(1);
        verify(futures.get(0), never()).cancel(anyBoolean());
    }

    @Test
    void theOwnersReconnectReplacesTheEmitterCancelsTheOldTasksAndKeepsSubscriptions() throws Exception {
        List<ScheduledFuture<?>> futures = distinctFutures();
        openSeeded("sess-1", "alice", System.currentTimeMillis() + 60_000L, List.of("Goal:1"));
        Object first = localEmitters().get("sess-1");

        streamService.createStream("sess-1", "alice", System.currentTimeMillis() + 60_000L, List.of("Story:2"));

        Object second = localEmitters().get("sess-1");
        assertThat(second).isNotSameAs(first);
        assertThat(localEmitters()).hasSize(1);
        // The first stream's keep-alive and expiry tasks are cancelled; the second's aren't.
        assertThat(futures).hasSize(4);
        verify(futures.get(0)).cancel(false);
        verify(futures.get(1)).cancel(false);
        verify(futures.get(2), never()).cancel(anyBoolean());
        verify(futures.get(3), never()).cancel(anyBoolean());
        assertThat(sessionStore.getSessionOwner("sess-1")).isEqualTo("alice");
        assertThat(sessionStore.getSubscriptions("sess-1")).containsExactlyInAnyOrder("Goal:1", "Story:2");
    }

    @Test
    void anUnknownSessionIdGetsANewServerGeneratedSession() throws Exception {
        streamService.createStream("made-up-id", "alice", 0L, List.of());

        assertThat(sessionStore.getSessionOwner("made-up-id")).isNull();
        assertThat(sessionStore.getActiveSessionCount()).isEqualTo(1);
        assertThat(localEmitters()).doesNotContainKey("made-up-id").hasSize(1);
    }

    @Test
    void unreadableInitialSubscriptionsAreSkippedAndTheStreamStillOpens() {
        StreamService restricted = new StreamService(sessionStore, taskScheduler, new ObjectMapper(),
                new SimpleMeterRegistry(), (user, key) -> key.equals("Project:0"));

        SseEmitter emitter = restricted.createStream(null, "alice", 0L, List.of("Project:0", "Goal:9"));

        assertThat(emitter).isNotNull();
        assertThat(sessionStore.getSessionsForTarget("Project:0")).hasSize(1);
        assertThat(sessionStore.getSessionsForTarget("Goal:9")).isEmpty();
    }

    @Test
    void verifyCanSubscribeRefusesWithForbidden() {
        StreamService restricted = new StreamService(sessionStore, taskScheduler, new ObjectMapper(),
                new SimpleMeterRegistry(), (user, key) -> false);

        assertThatThrownBy(() -> restricted.verifyCanSubscribe("alice", "Goal:9"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        assertThatNoException().isThrownBy(() -> streamService.verifyCanSubscribe("alice", "Goal:9"));
    }

    @Test
    void concurrentReconnectsLeaveOneLiveEmitterAndCancelEveryReplacedOne() throws Exception {
        List<ScheduledFuture<?>> futures = distinctFutures();
        openSeeded("sess-1", "alice", 0L, List.of("Goal:1"));

        int threads = 8;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<?>> done = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            done.add(pool.submit(() -> {
                start.await();
                return streamService.createStream("sess-1", "alice", 0L, List.of());
            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<?> f : done) {
            f.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(localEmitters()).hasSize(1);
        Object live = localEmitters().get("sess-1");
        Object liveKeepAlive = component(live, "keepAlive");
        assertThat(futures).hasSize(threads + 1);
        // A replaced holder can be cancelled twice: once by the reconnect that replaced it, and
        // again when its own Session event then fails on its completed emitter. Cancelling is
        // idempotent; what matters is that every replaced one is cancelled and the live one isn't.
        for (ScheduledFuture<?> f : futures) {
            if (f == liveKeepAlive) {
                verify(f, never()).cancel(anyBoolean());
            } else {
                verify(f, atLeastOnce()).cancel(false);
            }
        }
        assertThat(sessionStore.getSessionOwner("sess-1")).isEqualTo("alice");
        assertThat(sessionStore.getSubscriptions("sess-1")).containsExactly("Goal:1");
    }
}
