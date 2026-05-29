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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.assistant.api.EntityRef;

class AiAnalysisRequestTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void copiesMutableCollectionsAtConstruction() {
		List<Object> packs = new ArrayList<Object>();
		packs.add("pack");
		Map<String, Object> flags = new HashMap<String, Object>();
		flags.put("externalProviderAllowed", false);
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put("templateId", "requirements-review");

		AiAnalysisRequest request = new AiAnalysisRequest("assistant", UUID.randomUUID(),
				"REQUIREMENTS_REVIEW", EntityRef.of("Goal", 1L), EntityRef.of("Project", 2L),
				Locale.US, packs, "requirements-review-output", "1",
				objectMapper.createObjectNode(), flags, attrs);

		packs.clear();
		flags.clear();
		attrs.clear();

		assertThat(request.contextPacks()).containsExactly("pack");
		assertThat(request.dataHandlingFlags()).containsEntry("externalProviderAllowed", false);
		assertThat(request.attributes()).containsEntry("templateId", "requirements-review");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				request.contextPacks().add("other"));
	}
}
