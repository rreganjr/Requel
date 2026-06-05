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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.rreganjr.requel.assistant.api.AssistantMessage;

/**
 * Provider-neutral structured AI output. Provider clients validate raw model
 * output against the requested schema before constructing this response.
 */
public record AiAnalysisResponse(String summary, JsonNode structuredOutput,
		List<AiFindingDraft> findings, List<AssistantMessage> messages, AiUsage usage,
		Map<String, Object> providerMetadata) {

	public AiAnalysisResponse {
		Objects.requireNonNull(summary, "summary");
		Objects.requireNonNull(structuredOutput, "structuredOutput");
		findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
		messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
		Objects.requireNonNull(usage, "usage");
		providerMetadata = Map.copyOf(Objects.requireNonNull(providerMetadata,
				"providerMetadata"));
	}
}
