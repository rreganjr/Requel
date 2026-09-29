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
package com.rreganjr.requel.assistant.legacynlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;

/** Issue #268: the per-property wrapper the four lexical checks share. */
class PropertyChecksTest {

	private final PropertyChecks checks = new PropertyChecks(
			LoggerFactory.getLogger(PropertyChecksTest.class), "test", EntityRef.of("Goal", 1L));

	@Test
	void aBlankOrMissingPropertyIsCompleteWithoutRunningTheCheck() {
		checks.run("Name", null, () -> {
			throw new AssertionError("not run for a null property");
		});
		checks.run("Text", "  ", () -> {
			throw new AssertionError("not run for a blank property");
		});

		assertThat(checks.completed("Name")).isTrue();
		assertThat(checks.completed("Text")).isTrue();
		assertThat(checks.complete()).isTrue();
		assertThat(checks.failedProperties()).isEmpty();
	}

	@Test
	void aPropertyThatThrowsIsRecordedAndTheResultIsMarkedIncomplete() {
		checks.run("Name", "fine", () -> {
		});
		checks.run("Text", "broken", () -> {
			throw new IllegalStateException("parser failed");
		});

		assertThat(checks.completed("Name")).isTrue();
		assertThat(checks.completed("Text")).isFalse();
		assertThat(checks.complete()).isFalse();
		assertThat(checks.failedProperties()).containsExactly("Text");
		AssistantResult result = checks.finish(AssistantResult.builder().assistantId("test")).build();
		assertThat(result.metadata()).isEqualTo(Map.of("incomplete", Boolean.TRUE,
				"failedProperties", List.of("Text")));
	}

	@Test
	void aCompleteRunAddsNoMetadata() {
		checks.run("Name", "fine", () -> {
		});
		AssistantResult result = checks.finish(AssistantResult.builder().assistantId("test")).build();
		assertThat(result.metadata()).isEmpty();
	}
}
