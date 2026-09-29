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
package com.rreganjr.requel.assistant.core.freshness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.Goal;

/** Issue #270: the fingerprint rule. */
class TargetFingerprintTest {

	@Test
	void theSameNameAndTextGiveTheSameFingerprint() {
		assertThat(TargetFingerprint.of("Login", "Users log in."))
				.isEqualTo(TargetFingerprint.of("Login", "Users log in."))
				.hasSize(64)
				.matches("[0-9a-f]{64}");
	}

	@Test
	void whitespaceOnlyEditsDoNotChangeIt() {
		assertThat(TargetFingerprint.of("  Login ", "Users\n  log\tin."))
				.isEqualTo(TargetFingerprint.of("Login", "Users log in."));
	}

	@Test
	void aNameChangeOrATextChangeChangesIt() {
		String original = TargetFingerprint.of("Login", "Users log in.");
		assertThat(TargetFingerprint.of("Log in", "Users log in.")).isNotEqualTo(original);
		assertThat(TargetFingerprint.of("Login", "Users sign in.")).isNotEqualTo(original);
	}

	@Test
	void caseIsKept() {
		assertThat(TargetFingerprint.of("login", "users log in."))
				.isNotEqualTo(TargetFingerprint.of("Login", "Users log in."));
	}

	@Test
	void nameAndTextAreKeptApart() {
		assertThat(TargetFingerprint.of("ab", "c")).isNotEqualTo(TargetFingerprint.of("a", "bc"));
	}

	@Test
	void nullReadsAsEmpty() {
		assertThat(TargetFingerprint.of(null, null)).isEqualTo(TargetFingerprint.of("", ""));
		assertThat(TargetFingerprint.of("Login", null)).isEqualTo(TargetFingerprint.of("Login", ""));
	}

	@Test
	void anEntityIsFingerprintedFromItsNameAndText() {
		Goal goal = mock(Goal.class);
		when(goal.getName()).thenReturn("Login");
		when(goal.getText()).thenReturn("Users log in.");
		assertThat(TargetFingerprint.of(goal)).isEqualTo(TargetFingerprint.of("Login", "Users log in."));
	}

	@Test
	void somethingWithNeitherANameNorATextHasNoFingerprint() {
		assertThat(TargetFingerprint.of(new Object())).isNull();
		assertThat(TargetFingerprint.of((Object) null)).isNull();
	}
}
