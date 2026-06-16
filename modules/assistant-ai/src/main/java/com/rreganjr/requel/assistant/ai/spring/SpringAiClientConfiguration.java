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
package com.rreganjr.requel.assistant.ai.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiProperties;

/**
 * Wires the single Spring AI-backed {@link com.rreganjr.requel.assistant.ai.AiAnalysisClient},
 * active when an OpenAI (or OpenAI-compatible local) provider is selected. Mutually exclusive with
 * {@link com.rreganjr.requel.assistant.ai.NoopAiAnalysisClient} (active for {@code noop}/missing),
 * so exactly one client bean exists. {@code openai}, {@code openai-compat}, and {@code anthropic}
 * all route to this same provider-agnostic client — the difference is purely Spring AI config: the
 * OpenAI-style providers vary {@code spring.ai.openai.*} (notably {@code base-url} for Ollama/Gemini),
 * while {@code anthropic} sets {@code spring.ai.model.chat=anthropic} so the Anthropic ChatModel is
 * the single active one (see the {@code ai-*} profiles).
 *
 * <p>
 * Kept separate from {@link com.rreganjr.requel.assistant.ai.AiConfiguration} so property-binding
 * tests can load the configuration-properties class without requiring a {@code ChatClient.Builder}
 * on the context.
 */
@Configuration
public class SpringAiClientConfiguration {

	@Bean
	@ConditionalOnExpression("'${requel.ai.provider:noop}' == 'openai' "
			+ "or '${requel.ai.provider:noop}' == 'openai-compat' "
			+ "or '${requel.ai.provider:noop}' == 'anthropic'")
	public SpringAiAnalysisClient springAiAnalysisClient(
			ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, AiProperties properties,
			ObjectMapper objectMapper) {
		// ObjectProvider (not the builder directly) so this bean does not eagerly depend on the
		// ChatClient.Builder — that dependency closes a cycle through Spring AI tool-calling and the
		// MCP ToolCallbackProvider. SpringAiAnalysisClient resolves it lazily on first use.
		return new SpringAiAnalysisClient(chatClientBuilderProvider, properties, objectMapper);
	}
}
