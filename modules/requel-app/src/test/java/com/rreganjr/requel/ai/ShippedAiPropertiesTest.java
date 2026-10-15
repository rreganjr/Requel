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
package com.rreganjr.requel.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Guards on the shipped configuration (#259): the default {@code application.properties} never
 * selects the dev-only {@code cli} provider, and the {@code ai-cli} profile selects it without
 * pointing at a real binary or asking Spring AI for a chat model.
 */
class ShippedAiPropertiesTest {

	private static Properties load(String resource) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = ShippedAiPropertiesTest.class.getResourceAsStream(resource)) {
			assertThat(in).as(resource + " on the classpath").isNotNull();
			properties.load(in);
		}
		return properties;
	}

	@Test
	void theDefaultConfigurationDoesNotSelectTheCliProvider() throws Exception {
		Properties shipped = load("/application.properties");

		assertThat(shipped.getProperty("requel.ai.provider", "noop")).isNotEqualTo("cli");
		assertThat(shipped.getProperty("requel.ai.enabled")).isEqualTo("false");
		assertThat(shipped.stringPropertyNames()).noneMatch(name -> name.startsWith("requel.ai.cli."));
	}

	@Test
	void theAiCliProfileSelectsCliWithoutAChatModelOrABinary() throws Exception {
		Properties profile = load("/application-ai-cli.properties");

		assertThat(profile.getProperty("requel.ai.provider")).isEqualTo("cli");
		assertThat(profile.getProperty("requel.ai.enabled")).isEqualTo("true");
		assertThat(profile.getProperty("spring.ai.model.chat")).isEqualTo("none");
		assertThat(profile.getProperty("requel.ai.cli.command")).isEqualTo("${REQUEL_AI_CLI_COMMAND:}");
		assertThat(profile.getProperty("requel.ai.cli.args[3]")).isEqualTo("--tools");
		assertThat(profile.getProperty("requel.ai.cli.args[4]")).isEmpty();
		assertThat(profile.getProperty("requel.ai.cli.args[5]")).isEqualTo("--strict-mcp-config");
	}
}
