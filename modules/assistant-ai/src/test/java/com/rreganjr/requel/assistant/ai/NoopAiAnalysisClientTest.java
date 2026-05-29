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
package com.rreganjr.requel.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.AssistantMessage.Level;

class NoopAiAnalysisClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void returnsDeterministicEmptyResponseWithoutNetworkWork() throws Exception {
		AiProperties properties = new AiProperties();
		properties.setModel("noop-model");
		NoopAiAnalysisClient client = new NoopAiAnalysisClient(properties, objectMapper);

		AiAnalysisResponse response = client.analyze(request());

		assertThat(response.summary()).contains("noop");
		assertThat(response.structuredOutput().path("findings")).isEmpty();
		assertThat(response.findings()).isEmpty();
		assertThat(response.messages()).hasSize(1);
		assertThat(response.messages().get(0).level()).isEqualTo(Level.INFO);
		assertThat(response.usage().provider()).isEqualTo("noop");
		assertThat(response.usage().model()).isEqualTo("noop-model");
		assertThat(response.usage().inputTokens()).isZero();
		assertThat(response.providerMetadata()).containsEntry("provider", "noop");
	}

	private AiAnalysisRequest request() {
		return new AiAnalysisRequest("assistant", UUID.randomUUID(), "REQUIREMENTS_REVIEW",
				EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L), Locale.US, List.of(),
				"requirements-review-output", "1", objectMapper.createObjectNode(), Map.of(),
				Map.of());
	}
}
