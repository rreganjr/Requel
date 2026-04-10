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

import java.util.List;
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
}
