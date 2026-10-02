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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Issue #260: the output schemas a definition may name, loaded once from the classpath. The set
 * matches {@code AssistantDefinitionValidator.OUTPUT_SCHEMAS}.
 */
final class OutputSchemas {

	private static final Map<String, String> RESOURCES = Map.of(
			RequirementsReview.OUTPUT_SCHEMA_NAME + ":" + RequirementsReview.OUTPUT_SCHEMA_VERSION,
			"/ai/schemas/requirements-review-output.v1.json",
			// #263: v1 plus suggestedEntityName, for extraction findings' one-click positions
			RequirementsReview.OUTPUT_SCHEMA_NAME + ":2",
			"/ai/schemas/requirements-review-output.v2.json");

	private final ObjectMapper objectMapper;
	private final Map<String, JsonNode> loaded = new ConcurrentHashMap<>();

	OutputSchemas(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @throws IllegalStateException when the schema is not in the allowed set or can't be read
	 */
	JsonNode schema(String name, String version) {
		String key = name + ":" + version;
		return loaded.computeIfAbsent(key, this::load);
	}

	private JsonNode load(String key) {
		String resource = RESOURCES.get(key);
		if (resource == null) {
			throw new IllegalStateException("unknown output schema " + key);
		}
		try (InputStream in = OutputSchemas.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("output schema resource not found: " + resource);
			}
			return objectMapper.readTree(in);
		} catch (IOException e) {
			throw new IllegalStateException("could not read output schema " + resource, e);
		}
	}
}
