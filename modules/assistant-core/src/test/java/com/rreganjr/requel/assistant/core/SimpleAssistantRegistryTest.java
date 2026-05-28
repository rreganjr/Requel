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
package com.rreganjr.requel.assistant.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.assistant.api.UserRef;

class SimpleAssistantRegistryTest {

	@Test
	void findsAssistantsByTargetType() {
		SimpleAssistantRegistry registry = new SimpleAssistantRegistry(
				List.of(new NumberAssistant(), new StringAssistant()));

		assertThat(registry.findAssistantsFor("goal", context())).extracting(
				RequelAssistant::assistantId).containsExactly("string-assistant");
	}

	private AssistantContext context() {
		return new AssistantContext(UUID.randomUUID(), new UserRef(2L, "human"),
				new UserRef(3L, "assistant"), EntityRef.of("Project", 1L), java.util.Locale.US,
				Clock.systemUTC(), Map.of());
	}

	private static final class StringAssistant implements RequelAssistant<String> {
		@Override
		public String assistantId() {
			return "string-assistant";
		}

		@Override
		public Class<String> targetType() {
			return String.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, String target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}

	private static final class NumberAssistant implements RequelAssistant<Number> {
		@Override
		public String assistantId() {
			return "number-assistant";
		}

		@Override
		public Class<Number> targetType() {
			return Number.class;
		}

		@Override
		public AssistantResult analyze(AssistantContext context, Number target) {
			return AssistantResult.builder().assistantId(assistantId()).build();
		}
	}
}
