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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.AssistantMessage;

/**
 * Deterministic local/default client. It performs no network work and returns
 * a valid empty structured response so AI call sites can be tested safely.
 *
 * <p>
 * Active unless an external provider is selected. This is gated on
 * {@code requel.ai.provider} (default/missing or {@code noop}) rather than
 * {@code @ConditionalOnMissingBean}, because component-scanned beans are registered in an
 * undefined order — the missing-bean check could run before a provider client (e.g.
 * {@link com.rreganjr.requel.assistant.openai.OpenAiAnalysisClient OpenAiAnalysisClient}, gated
 * on {@code requel.ai.provider=openai}) is registered, leaving two {@code AiAnalysisClient}
 * beans and an ambiguous injection. The property conditions are mutually exclusive, so exactly
 * one client exists.
 */
@Component
@ConditionalOnProperty(prefix = "requel.ai", name = "provider", havingValue = "noop",
		matchIfMissing = true)
public class NoopAiAnalysisClient implements AiAnalysisClient {

	private final AiProperties properties;
	private final ObjectMapper objectMapper;

	@Autowired
	public NoopAiAnalysisClient(AiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public AiAnalysisResponse analyze(AiAnalysisRequest request) {
		Instant startedAt = Instant.now();
		JsonNode output = objectMapper.valueToTree(Map.of(
				"summary", "AI analysis is disabled or configured for noop.",
				"findings", List.of()));
		return new AiAnalysisResponse("AI analysis is disabled or configured for noop.", output,
				List.of(),
				List.of(AssistantMessage.info("AI analysis is disabled or configured for noop.")),
				AiUsage.noop(properties.getModel(), Duration.between(startedAt, Instant.now())),
				Map.of("provider", properties.getProvider(), "enabled", properties.isEnabled()));
	}
}
