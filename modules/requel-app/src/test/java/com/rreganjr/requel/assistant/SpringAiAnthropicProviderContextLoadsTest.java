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
package com.rreganjr.requel.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.requel.Application;
import com.rreganjr.requel.assistant.ai.AiAnalysisClient;
import com.rreganjr.requel.assistant.ai.spring.SpringAiAnalysisClient;

/**
 * Regression guard for the Anthropic provider (issue #77 fast-follow, the {@code ai-anthropic}
 * profile): the full application context must boot when {@code requel.ai.provider=anthropic} is
 * selected, and the active {@link AiAnalysisClient} must be the provider-agnostic
 * {@link SpringAiAnalysisClient}.
 *
 * <p>
 * Anthropic uses its own {@code spring-ai-starter-model-anthropic} (no OpenAI-compatible endpoint),
 * selected by {@code spring.ai.model.chat=anthropic} so the Anthropic ChatModel is the single active
 * one. This reproduces the same tool-calling wiring the OpenAI guard checks
 * ({@link SpringAiProviderContextLoadsTest}); if a bean cycle ever returns through the Anthropic
 * ChatModel's ToolCallingManager, this context fails to start. No network: the client builds its
 * ChatClient lazily and no review is triggered; the placeholder {@code spring.ai.anthropic.api-key}
 * in application-test.properties keeps the Anthropic autoconfiguration satisfied.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"requel.ai.provider=anthropic",
		"spring.ai.model.chat=anthropic" })
public class SpringAiAnthropicProviderContextLoadsTest {

	@Autowired
	private AiAnalysisClient aiAnalysisClient;

	@Test
	void contextLoadsWithSpringAiClientWhenAnthropicSelected() {
		assertThat(aiAnalysisClient).isInstanceOf(SpringAiAnalysisClient.class);
	}
}
