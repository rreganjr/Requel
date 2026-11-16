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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.AssistantRole;
import com.rreganjr.requel.user.User;

/**
 * {@link PasswordCheck} does a real check's hashing work for an account that can't sign in
 * (issue #448), so a failure takes the same time whether or not the username exists. Checked by
 * which path runs, not by timing.
 */
class PasswordCheckTest {

    private final PasswordCheck check = spy(new PasswordCheck());

    @Test
    void anUnknownUserFailsAfterTheDummyHash() {
        assertThat(check.matches(null, "guess")).isFalse();
        verify(check).dummyCheck("guess");
    }

    @Test
    void anAssistantAccountFailsAfterTheDummyHash() {
        User assistant = mock(User.class);
        when(assistant.hasRole(AssistantRole.class)).thenReturn(true);
        assertThat(check.matches(assistant, "assistant")).isFalse();
        verify(check).dummyCheck("assistant");
        verify(assistant, never()).isPassword("assistant");
    }

    @Test
    void aRealUserIsCheckedAgainstTheirOwnPassword() {
        User alice = mock(User.class);
        when(alice.isPassword("secret")).thenReturn(true);
        assertThat(check.matches(alice, "secret")).isTrue();
        assertThat(check.matches(alice, "wrong")).isFalse();
        verify(check, never()).dummyCheck("secret");
        verify(check, never()).dummyCheck("wrong");
    }

    @Test
    void theDummyHashToleratesANullOrOverlongPassword() {
        assertThat(check.matches(null, null)).isFalse();
        assertThat(check.matches(null, "x".repeat(10_000))).isFalse();
    }
}
