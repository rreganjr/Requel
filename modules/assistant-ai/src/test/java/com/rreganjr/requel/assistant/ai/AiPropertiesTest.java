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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiPropertiesTest {

	/**
	 * Pure-POJO assertion of the defaults — no Spring context and no environment, so ambient
	 * {@code REQUEL_AI_*} variables on the build machine cannot influence it.
	 */
	@Test
	void defaultsToDisabledNoopProvider() {
		AiProperties properties = new AiProperties();

		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getProvider()).isEqualTo("noop");
		assertThat(properties.getModel()).isEqualTo("noop");
		assertThat(properties.getMaxInputTokens()).isEqualTo(16000);
		assertThat(properties.getProjectAllowlist()).isEmpty();
	}

	/**
	 * Binding still works through Spring. Inlined test property values are the highest-precedence
	 * property source, so they override any ambient {@code REQUEL_AI_*} environment variables.
	 */
	@Test
	void bindsConfiguredValues() {
		new ApplicationContextRunner()
				.withUserConfiguration(AiConfiguration.class)
				.withPropertyValues(
						"requel.ai.enabled=true",
						"requel.ai.provider=openai",
						"requel.ai.model=gpt-test",
						"requel.ai.max-input-tokens=2000",
						"requel.ai.project-allowlist=Alpha,Beta")
				.run(context -> {
					AiProperties properties = context.getBean(AiProperties.class);

					assertThat(properties.isEnabled()).isTrue();
					assertThat(properties.getProvider()).isEqualTo("openai");
					assertThat(properties.getModel()).isEqualTo("gpt-test");
					assertThat(properties.getMaxInputTokens()).isEqualTo(2000);
					assertThat(properties.getProjectAllowlist()).containsExactly("Alpha", "Beta");
				});
	}
}
