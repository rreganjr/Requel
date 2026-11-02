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
package com.rreganjr.requel.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * {@link LoginThrottle}'s counting, waits and recovery (issue #448), on a clock the test moves.
 * The limits are the shipped defaults: 5 per username + source, 50 per source, 50 per username,
 * a 15-minute window and a 15-minute longest wait.
 */
class LoginThrottleTest {

    private static final String SOURCE = "203.0.113.7";

    private MutableClock clock;
    private SimpleMeterRegistry meters;
    private LoginThrottle throttle;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-01T12:00:00Z"));
        meters = new SimpleMeterRegistry();
        throttle = throttle(true);
    }

    @SuppressWarnings("unchecked")
    private LoginThrottle throttle(boolean enabled) {
        ObjectProvider<Clock> clocks = mock(ObjectProvider.class);
        when(clocks.getIfAvailable(any())).thenReturn(clock);
        ObjectProvider<MeterRegistry> registries = mock(ObjectProvider.class);
        when(registries.getIfAvailable()).thenReturn(meters);
        return new LoginThrottle(enabled, Duration.ofMinutes(15), Duration.ofMinutes(15), 5, 50, 50,
                clocks, registries);
    }

    private void fail(String username, String source, int times) {
        for (int i = 0; i < times; i++) {
            throttle.recordFailure(username, source);
        }
    }

    @Test
    void fourFailuresAreAllowedAndTheFifthStartsAOneSecondWait() {
        fail("alice", SOURCE, 4);
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
        fail("alice", SOURCE, 1);
        LoginThrottle.Refusal refusal = throttle.check("api", "alice", SOURCE);
        assertThat(refusal).isNotNull();
        assertThat(refusal.retryAfterSeconds()).isEqualTo(1);
        assertThat(refusal.limit()).isEqualTo(LoginThrottle.Limit.PAIR);
        clock.advance(Duration.ofSeconds(1));
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
    }

    @Test
    void theWaitDoublesWithEachFailureUpToFifteenMinutes() {
        fail("alice", SOURCE, 5);
        assertThat(throttle.check("api", "alice", SOURCE).retryAfterSeconds()).isEqualTo(1);
        fail("alice", SOURCE, 1);
        assertThat(throttle.check("api", "alice", SOURCE).retryAfterSeconds()).isEqualTo(2);
        fail("alice", SOURCE, 1);
        assertThat(throttle.check("api", "alice", SOURCE).retryAfterSeconds()).isEqualTo(4);
        fail("alice", SOURCE, 20);
        assertThat(throttle.check("api", "alice", SOURCE).retryAfterSeconds()).isEqualTo(15 * 60);
    }

    @Test
    void theCountClearsOnceAWindowPassesWithNoFailures() {
        fail("alice", SOURCE, 4);
        clock.advance(Duration.ofMinutes(15));
        fail("alice", SOURCE, 4);
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
    }

    @Test
    void theRightPasswordWorksAgainOnceTheLongestWaitIsOver() {
        fail("alice", SOURCE, 40);
        assertThat(throttle.check("api", "alice", SOURCE).retryAfterSeconds()).isEqualTo(15 * 60);
        clock.advance(Duration.ofMinutes(15));
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
    }

    @Test
    void aSuccessClearsTheUsernameAndSourceCount() {
        fail("alice", SOURCE, 4);
        throttle.recordSuccess("alice", SOURCE);
        fail("alice", SOURCE, 4);
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
    }

    @Test
    void oneUsernamesFailuresDontSlowAnotherOrAnotherSource() {
        fail("alice", SOURCE, 5);
        assertThat(throttle.check("api", "alice", SOURCE)).isNotNull();
        assertThat(throttle.check("api", "bob", SOURCE)).isNull();
        assertThat(throttle.check("api", "alice", "198.51.100.9")).isNull();
    }

    @Test
    void usernamesMatchIgnoringCaseAndSurroundingSpaces() {
        fail("Alice", SOURCE, 3);
        fail(" alice ", SOURCE, 2);
        assertThat(throttle.check("api", "ALICE", SOURCE)).isNotNull();
    }

    @Test
    void oneSourceTryingManyUsernamesIsStoppedForFifteenMinutes() {
        for (int i = 0; i < 50; i++) {
            throttle.recordFailure("user" + i, SOURCE);
        }
        LoginThrottle.Refusal refusal = throttle.check("api", "someone-new", SOURCE);
        assertThat(refusal).isNotNull();
        assertThat(refusal.limit()).isEqualTo(LoginThrottle.Limit.SOURCE);
        assertThat(refusal.retryAfterSeconds()).isEqualTo(15 * 60);
        assertThat(throttle.check("api", "someone-new", "198.51.100.9")).isNull();
    }

    @Test
    void manySourcesTryingOneUsernameAreSlowedOnThatUsername() {
        for (int i = 0; i < 50; i++) {
            throttle.recordFailure("alice", "10.0.0." + i);
        }
        LoginThrottle.Refusal refusal = throttle.check("api", "alice", "10.0.1.1");
        assertThat(refusal).isNotNull();
        assertThat(refusal.limit()).isEqualTo(LoginThrottle.Limit.USERNAME);
        assertThat(refusal.retryAfterSeconds()).isEqualTo(1);
        assertThat(throttle.check("api", "bob", "10.0.1.1")).isNull();
    }

    @Test
    void anUnknownUsernameIsCountedLikeAnyOther() {
        fail("no-such-user", SOURCE, 5);
        assertThat(throttle.check("api", "no-such-user", SOURCE)).isNotNull();
    }

    @Test
    void aRefusalIsCountedInTheMetricByPathAndLimit() {
        fail("alice", SOURCE, 5);
        throttle.check("form", "alice", SOURCE);
        throttle.check("form", "alice", SOURCE);
        assertThat(meters.counter(LoginThrottle.THROTTLED_METRIC, "path", "form",
                "limit", "username-and-source").count()).isEqualTo(2.0);
    }

    @Test
    void theMessageSaysHowLongToWait() {
        assertThat(new LoginThrottle.Refusal(1, LoginThrottle.Limit.PAIR).message())
                .isEqualTo("Too many sign-in attempts. Try again in 1 second.");
        assertThat(new LoginThrottle.Refusal(8, LoginThrottle.Limit.PAIR).message())
                .isEqualTo("Too many sign-in attempts. Try again in 8 seconds.");
        assertThat(new LoginThrottle.Refusal(61, LoginThrottle.Limit.PAIR).message())
                .isEqualTo("Too many sign-in attempts. Try again in 2 minutes.");
        assertThat(new LoginThrottle.Refusal(900, LoginThrottle.Limit.SOURCE).message())
                .isEqualTo("Too many sign-in attempts. Try again in 15 minutes.");
    }

    @Test
    void switchedOffItNeverRefuses() {
        throttle = throttle(false);
        fail("alice", SOURCE, 100);
        assertThat(throttle.check("api", "alice", SOURCE)).isNull();
    }

    @Test
    void expiredEntriesAreSweptSoTheMapsStayBounded() {
        for (int i = 0; i < 20; i++) {
            throttle.recordFailure("user" + i, "10.0.0." + i);
        }
        clock.advance(Duration.ofMinutes(16));
        throttle.recordFailure("late", SOURCE);
        // only "late" is left: its pair, its source and its username
        assertThat(throttle.trackedKeys()).isEqualTo(3);
    }

    /** A clock the test moves forward. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
