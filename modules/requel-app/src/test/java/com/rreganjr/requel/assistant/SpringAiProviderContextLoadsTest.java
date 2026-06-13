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
 * Regression guard for the Spring AI provider port (issue #77): the full application context must
 * boot when an OpenAI-family provider is actually selected, and the active {@link AiAnalysisClient}
 * must be the {@link SpringAiAnalysisClient}.
 *
 * <p>
 * The rest of the suite runs with {@code requel.ai.provider=noop}, so the Spring AI client bean is
 * never created and never exercises its wiring. That is exactly how the bean cycle introduced by
 * the port slipped through: {@code springAiAnalysisClient -> ChatClient.Builder -> openAiChatModel
 * -> ToolCallingManager -> the MCP ToolCallbackProvider -> the gateway/command chain -> the
 * assistant -> springAiAnalysisClient}. Pinning {@code provider=openai-compat} here reproduces that
 * wiring; if the cycle ever returns, this context fails to start. (No network: the client builds
 * its ChatClient lazily on first use, and no review is triggered. The placeholder
 * {@code spring.ai.openai.api-key} default in application.properties keeps the OpenAI
 * autoconfiguration satisfied.)
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "requel.ai.provider=openai-compat")
public class SpringAiProviderContextLoadsTest {

	@Autowired
	private AiAnalysisClient aiAnalysisClient;

	@Test
	void contextLoadsWithSpringAiClientWhenOpenAiCompatSelected() {
		assertThat(aiAnalysisClient).isInstanceOf(SpringAiAnalysisClient.class);
	}
}
