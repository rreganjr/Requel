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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Bounded, provider-neutral input for an AI analysis call. Context pack objects
 * are deliberately typed as {@code Object} so this module can carry project,
 * entity, and issue context packs without forcing provider modules to know a
 * single concrete prompt shape.
 */
public record AiAnalysisRequest(String assistantId, UUID runId, String taskType,
		EntityRef targetRef, EntityRef projectRef, Locale locale, List<Object> contextPacks,
		String outputSchemaName, String outputSchemaVersion, JsonNode outputSchema,
		Map<String, Object> dataHandlingFlags, Map<String, Object> attributes) {

	public AiAnalysisRequest {
		Objects.requireNonNull(assistantId, "assistantId");
		Objects.requireNonNull(runId, "runId");
		Objects.requireNonNull(taskType, "taskType");
		Objects.requireNonNull(targetRef, "targetRef");
		Objects.requireNonNull(projectRef, "projectRef");
		Objects.requireNonNull(locale, "locale");
		contextPacks = List.copyOf(Objects.requireNonNull(contextPacks, "contextPacks"));
		Objects.requireNonNull(outputSchemaName, "outputSchemaName");
		Objects.requireNonNull(outputSchemaVersion, "outputSchemaVersion");
		Objects.requireNonNull(outputSchema, "outputSchema");
		dataHandlingFlags = Map.copyOf(Objects.requireNonNull(dataHandlingFlags,
				"dataHandlingFlags"));
		attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
	}
}
