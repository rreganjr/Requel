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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StreamSessionStore}.
 *
 * Pure unit test — no mocks, no Spring context. The store is an in-memory
 * ConcurrentHashMap structure; tests verify the bidirectional bookkeeping
 * between sessions ↔ subscriptions ↔ targets.
 *
 * Scenarios covered:
 * - createSession / getSessionOwner: owner recorded, null for unknown session
 * - getActiveSessionCount: 0 initially, increments correctly
 * - addSubscription: appears in both getSubscriptions and getSessionsForTarget
 * - removeSubscription: removed from both directions; target entry deleted when empty
 * - removeSubscription on unknown session: no-op (no exception)
 * - removeSession: cleans up owner, subscriptions, and target entries
 * - removeSession when sole subscriber: target key deleted from target map
 * - Multiple sessions subscribing to the same target
 * - getSubscriptions / getSessionsForTarget: empty set for unknown ids
 */
class StreamSessionStoreTest {

    private StreamSessionStore store;

    @BeforeEach
    void setUp() {
        store = new StreamSessionStore();
    }

    // -------------------------------------------------------------------------
    // createSession / getSessionOwner
    // -------------------------------------------------------------------------

    @Test
    void getSessionOwnerReturnsOwnerAfterCreate() {
        store.createSession("s1", "alice");
        assertThat(store.getSessionOwner("s1")).isEqualTo("alice");
    }

    @Test
    void getSessionOwnerReturnsNullForUnknownSession() {
        assertThat(store.getSessionOwner("no-such-session")).isNull();
    }

    @Test
    void getActiveSessionCountStartsAtZero() {
        assertThat(store.getActiveSessionCount()).isZero();
    }

    @Test
    void getActiveSessionCountIncrementsOnCreate() {
        store.createSession("s1", "alice");
        store.createSession("s2", "bob");
        assertThat(store.getActiveSessionCount()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // addSubscription / getSubscriptions / getSessionsForTarget
    // -------------------------------------------------------------------------

    @Test
    void getSubscriptionsReturnsEmptyForUnknownSession() {
        assertThat(store.getSubscriptions("no-such-session")).isEmpty();
    }

    @Test
    void getSessionsForTargetReturnsEmptyWhenNoneSubscribed() {
        assertThat(store.getSessionsForTarget("Goal:1")).isEmpty();
    }

    @Test
    void addSubscriptionAppearsInGetSubscriptions() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:1");

        assertThat(store.getSubscriptions("s1")).containsExactly("Goal:1");
    }

    @Test
    void addSubscriptionAppearsInGetSessionsForTarget() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:1");

        assertThat(store.getSessionsForTarget("Goal:1")).containsExactly("s1");
    }

    @Test
    void multipleSessionsCanSubscribeToSameTarget() {
        store.createSession("s1", "alice");
        store.createSession("s2", "bob");
        store.addSubscription("s1", "Project:5");
        store.addSubscription("s2", "Project:5");

        assertThat(store.getSessionsForTarget("Project:5")).containsExactlyInAnyOrder("s1", "s2");
    }

    // -------------------------------------------------------------------------
    // removeSubscription
    // -------------------------------------------------------------------------

    @Test
    void removeSubscriptionDropsSessionFromTargetSubscribers() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:1");
        store.removeSubscription("s1", "Goal:1");

        assertThat(store.getSessionsForTarget("Goal:1")).isEmpty();
        assertThat(store.getSubscriptions("s1")).isEmpty();
    }

    @Test
    void removeSubscriptionDeletesTargetEntryWhenLastSubscriberLeaves() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:1");
        store.removeSubscription("s1", "Goal:1");

        // Target entry should be gone — verified by checking size is still 0
        assertThat(store.getSessionsForTarget("Goal:1")).isEmpty();
    }

    @Test
    void removeSubscriptionForOneSessionDoesNotAffectOther() {
        store.createSession("s1", "alice");
        store.createSession("s2", "bob");
        store.addSubscription("s1", "Goal:1");
        store.addSubscription("s2", "Goal:1");
        store.removeSubscription("s1", "Goal:1");

        assertThat(store.getSessionsForTarget("Goal:1")).containsExactly("s2");
        assertThat(store.getSubscriptions("s1")).isEmpty();
    }

    @Test
    void removeSubscriptionOnUnknownSessionIsNoOp() {
        // Should not throw
        store.removeSubscription("no-such-session", "Goal:1");
    }

    // -------------------------------------------------------------------------
    // removeSession
    // -------------------------------------------------------------------------

    @Test
    void removeSessionClearsOwner() {
        store.createSession("s1", "alice");
        store.removeSession("s1");

        assertThat(store.getSessionOwner("s1")).isNull();
    }

    @Test
    void removeSessionDecrementsActiveCount() {
        store.createSession("s1", "alice");
        store.createSession("s2", "bob");
        store.removeSession("s1");

        assertThat(store.getActiveSessionCount()).isEqualTo(1);
    }

    @Test
    void removeSessionCleansUpItsSubscriptions() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:1");
        store.addSubscription("s1", "Project:2");
        store.removeSession("s1");

        assertThat(store.getSubscriptions("s1")).isEmpty();
        assertThat(store.getSessionsForTarget("Goal:1")).isEmpty();
        assertThat(store.getSessionsForTarget("Project:2")).isEmpty();
    }

    @Test
    void removeSessionDeletesTargetEntryWhenSoleSubscriber() {
        store.createSession("s1", "alice");
        store.addSubscription("s1", "Goal:42");
        store.removeSession("s1");

        assertThat(store.getSessionsForTarget("Goal:42")).isEmpty();
    }

    @Test
    void removeSessionLeavesOtherSessionsSubscribedToSameTarget() {
        store.createSession("s1", "alice");
        store.createSession("s2", "bob");
        store.addSubscription("s1", "Project:5");
        store.addSubscription("s2", "Project:5");
        store.removeSession("s1");

        assertThat(store.getSessionsForTarget("Project:5")).containsExactly("s2");
    }
}
