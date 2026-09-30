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
package com.rreganjr.requel.assistant.ai.cli;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.ai.AiProperties;

/**
 * Registers {@link CliAiAnalysisClient} when {@code requel.ai.provider=cli}. The property
 * condition is mutually exclusive with the noop client ({@code noop}/missing) and the Spring AI
 * client ({@code openai}/{@code openai-compat}/{@code anthropic}), so exactly one
 * {@link com.rreganjr.requel.assistant.ai.AiAnalysisClient} exists. A cli provider that was asked
 * for but cannot run fails the context at boot rather than on the first review.
 *
 * <p>
 * <strong>Development only</strong>: this drives the developer's own interactively
 * authenticated CLI. See {@code doc/guides/AI_ASSISTANT_SETUP.md}.
 */
@Configuration
@ConditionalOnProperty(prefix = "requel.ai", name = "provider", havingValue = "cli")
@EnableConfigurationProperties(CliAiProperties.class)
public class CliAiClientConfiguration {

	@Bean
	public CliAiAnalysisClient cliAiAnalysisClient(AiProperties aiProperties,
			CliAiProperties cliProperties, ObjectMapper objectMapper) {
		cliProperties.validate();
		return new CliAiAnalysisClient(aiProperties, cliProperties, objectMapper,
				new ProcessBuilderCliProcessRunner(), System::getenv, Clock.systemUTC());
	}
}
