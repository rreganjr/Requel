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
package com.rreganjr.requel.assistant.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiAnalysisRequest;
import com.rreganjr.requel.assistant.ai.AiAnalysisResponse;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * End-to-end smoke test against the live OpenAI Responses API. It is skipped entirely unless
 * OPENAI_API_KEY is present in the environment, so it never runs (and never fails) in CI without
 * a key. When enabled, it exercises the full structured-output path: a real model call returning
 * JSON validated against the REQUIREMENTS_REVIEW output schema.
 *
 * Override the model via the OPENAI_MODEL environment variable; otherwise a small default is used.
 */
class OpenAiAnalysisClientLiveIT {

	private static final String OUTPUT_SCHEMA_RESOURCE =
			"/ai/schemas/requirements-review-output.v1.json";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
	void analyzesAgainstLiveOpenAi() throws Exception {
		AiProperties properties = new AiProperties();
		properties.setEnabled(true);
		properties.setProvider("openai");
		String model = System.getenv("OPENAI_MODEL");
		properties.setModel(model != null && !model.isBlank() ? model : "gpt-4o-mini");
		// This test is gated on OPENAI_API_KEY, so read the key from that variable explicitly
		// (the default key variable is now the provider-neutral REQUEL_AI_API_KEY). The endpoint
		// is left blank so the client falls back to its public Responses default.
		properties.setApiKeyEnvironmentVariable("OPENAI_API_KEY");

		OpenAiAnalysisClient client = new OpenAiAnalysisClient(properties, objectMapper);

		AiAnalysisResponse response = client.analyze(liveRequest());

		assertThat(response).isNotNull();
		assertThat(response.summary()).isNotBlank();
		assertThat(response.usage().provider()).isEqualTo("openai");
		assertThat(response.usage().model()).isEqualTo(properties.getModel());
	}

	private AiAnalysisRequest liveRequest() throws Exception {
		return new AiAnalysisRequest("ai-requirements-review", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				Locale.US,
				List.of(Map.of("kind", "target", "type", "Goal", "text",
						"The system should be fast and easy to use.")),
				"RequirementsReviewOutput", "1", loadSchema(),
				Map.of("externalProviderAllowed", true),
				Map.of("templateId", "requirements-review"));
	}

	private JsonNode loadSchema() throws Exception {
		try (InputStream in = getClass().getResourceAsStream(OUTPUT_SCHEMA_RESOURCE)) {
			if (in == null) {
				// The schema resource lives in assistant-ai; fall back to a minimal inline schema
				// so this test compiles and runs even if that resource is not on the test classpath.
				return objectMapper.readTree(
						"{\"type\":\"object\",\"additionalProperties\":false,"
								+ "\"properties\":{\"summary\":{\"type\":\"string\"},"
								+ "\"findings\":{\"type\":\"array\",\"items\":{\"type\":\"object\","
								+ "\"additionalProperties\":false,"
								+ "\"properties\":{\"findingType\":{\"type\":\"string\"}},"
								+ "\"required\":[\"findingType\"]}}},"
								+ "\"required\":[\"summary\",\"findings\"]}");
			}
			return objectMapper.readTree(in);
		}
	}
}
