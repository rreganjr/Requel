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
package com.rreganjr.platform.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * {@link DatabaseInitializer}'s two failure modes (issue #288): an ordinary failure is logged and
 * stepped over so one bad initializer cannot stop the system bootstrapping, and a
 * {@link FatalInitializationException} stops the chain.
 * <p>
 * Real initializers rather than mocks: {@link SystemInitializer} is {@link Comparable} and
 * {@code DatabaseInitializer} holds a {@code TreeSet}, so a mock's default {@code compareTo} of 0
 * would collapse every initializer into one element.
 *
 * @author ron
 */
class DatabaseInitializerTest {

	private static final class RecordingInitializer extends AbstractSystemInitializer {

		private final RuntimeException toThrow;
		private boolean ran;

		RecordingInitializer(int order, RuntimeException toThrow) {
			super(order);
			this.toThrow = toThrow;
		}

		@Override
		public void initialize() {
			ran = true;
			if (toThrow != null) {
				throw toThrow;
			}
		}
	}

	@Test
	void runsEveryInitializerInOrder() {
		RecordingInitializer first = new RecordingInitializer(1, null);
		RecordingInitializer second = new RecordingInitializer(2, null);

		new DatabaseInitializer(Set.of(first, second)).initialize();

		assertThat(first.ran).isTrue();
		assertThat(second.ran).isTrue();
	}

	@Test
	void anOrdinaryFailureDoesNotAbortTheChain() {
		RecordingInitializer failing = new RecordingInitializer(1,
				new IllegalStateException("something optional went wrong"));
		RecordingInitializer later = new RecordingInitializer(2, null);

		assertThatCode(() -> new DatabaseInitializer(Set.of(failing, later)).initialize())
				.doesNotThrowAnyException();

		assertThat(later.ran).as("user seeding ordered after a failure must still run").isTrue();
	}

	@Test
	void aFatalFailureAbortsTheChain() {
		RecordingInitializer fatal = new RecordingInitializer(1,
				new FatalInitializationException("the dictionary is empty"));
		RecordingInitializer later = new RecordingInitializer(2, null);

		assertThatThrownBy(() -> new DatabaseInitializer(Set.of(fatal, later)).initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("the dictionary is empty");

		assertThat(later.ran).as("nothing should run after a fatal initializer").isFalse();
	}
}
