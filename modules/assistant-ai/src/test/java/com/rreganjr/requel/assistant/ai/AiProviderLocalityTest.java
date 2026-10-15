/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Issue #262: which configured providers count as sending text off the machine. */
class AiProviderLocalityTest {

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
			"noop, null, LOCAL",
			"null, null, LOCAL",
			"cli, null, REMOTE",
			"openai, https://api.openai.com, REMOTE",
			"anthropic, null, REMOTE",
			"openai-compat, http://localhost:11434, LOCAL",
			"openai-compat, http://127.0.0.1:8000, LOCAL",
			"openai-compat, http://[::1]:11434, LOCAL",
			"openai-compat, http://host.docker.internal:11434, LOCAL",
			"openai-compat, https://generativelanguage.googleapis.com/v1beta/openai, REMOTE",
			"openai-compat, null, REMOTE",
			"openai-compat, not a url, REMOTE",
			"something-new, http://localhost, REMOTE"
	})
	void classifies(String provider, String baseUrl, AiProviderLocality expected) {
		assertThat(AiProviderLocality.classify(provider, baseUrl)).isEqualTo(expected);
	}
}
