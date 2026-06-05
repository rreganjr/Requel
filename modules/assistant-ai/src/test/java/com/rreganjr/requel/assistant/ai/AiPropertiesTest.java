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

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiPropertiesTest {

	@Test
	void defaultsToDisabledNoopProvider() {
		new ApplicationContextRunner().withUserConfiguration(AiConfiguration.class).run(context -> {
			AiProperties properties = context.getBean(AiProperties.class);

			assertThat(properties.isEnabled()).isFalse();
					assertThat(properties.getProvider()).isEqualTo("noop");
					assertThat(properties.getModel()).isEqualTo("noop");
					assertThat(properties.getApiKey()).isNull();
					assertThat(properties.getApiKeyEnvironmentVariable()).isEqualTo("REQUEL_AI_API_KEY");
					assertThat(properties.getEndpoint()).isEmpty();
					assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(30));
					assertThat(properties.getMaxRetries()).isEqualTo(2);
					assertThat(properties.getMaxInputTokens()).isEqualTo(16000);
					assertThat(properties.getMaxOutputTokens()).isEqualTo(4000);
					assertThat(properties.getStructuredOutputMode()).isEqualTo("json_object");
				});
	}

	@Test
	void bindsConfiguredValues() {
		new ApplicationContextRunner()
				.withUserConfiguration(AiConfiguration.class)
				.withPropertyValues(
						"requel.ai.enabled=true",
						"requel.ai.provider=openai",
						"requel.ai.model=gpt-test",
						"requel.ai.api-key=test-key",
						"requel.ai.api-key-environment-variable=TEST_OPENAI_KEY",
						"requel.ai.endpoint=http://localhost:9000/v1/responses",
						"requel.ai.timeout=5s",
						"requel.ai.max-retries=3",
						"requel.ai.max-input-tokens=2000",
						"requel.ai.max-output-tokens=500",
						"requel.ai.project-allowlist=Alpha,Beta")
				.run(context -> {
					AiProperties properties = context.getBean(AiProperties.class);

					assertThat(properties.isEnabled()).isTrue();
					assertThat(properties.getProvider()).isEqualTo("openai");
					assertThat(properties.getModel()).isEqualTo("gpt-test");
					assertThat(properties.getApiKey()).isEqualTo("test-key");
					assertThat(properties.getApiKeyEnvironmentVariable()).isEqualTo(
							"TEST_OPENAI_KEY");
					assertThat(properties.getEndpoint()).isEqualTo(
							"http://localhost:9000/v1/responses");
					assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(5));
					assertThat(properties.getMaxRetries()).isEqualTo(3);
					assertThat(properties.getMaxInputTokens()).isEqualTo(2000);
					assertThat(properties.getMaxOutputTokens()).isEqualTo(500);
					assertThat(properties.getProjectAllowlist()).containsExactly("Alpha", "Beta");
				});
	}
}
