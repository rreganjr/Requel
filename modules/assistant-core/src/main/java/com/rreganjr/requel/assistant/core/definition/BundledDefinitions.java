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
package com.rreganjr.requel.assistant.core.definition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Issue #260: reads the bundled definitions shipped as {@code classpath*:ai/definitions/*.json}
 * and validates them together. A bad file fails with a message naming the file and every
 * problem.
 *
 * <p>File format: {@code key, displayName, kind, taskType, version, scope[], contextProviders[],
 * outputSchemaName, outputSchemaVersion, enabled (default true), executorBean, vocabulary[{type,
 * description}], instructions}.
 */
public final class BundledDefinitions {

	public static final String LOCATION = "classpath*:ai/definitions/*.json";

	private BundledDefinitions() {
	}

	/**
	 * @throws IllegalStateException naming the file and the problems, when any file is unreadable,
	 *         invalid, or repeats a key
	 */
	public static List<AssistantDefinition> load(ObjectMapper objectMapper,
			AssistantDefinitionValidator validator) {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				BundledDefinitions.class.getClassLoader());
		Resource[] resources;
		try {
			resources = resolver.getResources(LOCATION);
		} catch (IOException e) {
			throw new IllegalStateException("could not list bundled assistant definitions at "
					+ LOCATION, e);
		}
		return load(objectMapper, validator, resources);
	}

	/** {@link #load(ObjectMapper, AssistantDefinitionValidator)} over the given files. */
	static List<AssistantDefinition> load(ObjectMapper objectMapper,
			AssistantDefinitionValidator validator, Resource[] resources) {
		List<AssistantDefinition> definitions = new ArrayList<AssistantDefinition>();
		Map<String, String> fileByKey = new HashMap<String, String>();
		for (Resource resource : resources) {
			String file = resource.getFilename();
			AssistantDefinition definition = parse(objectMapper, resource, file);
			String previous = fileByKey.put(definition.key(), file);
			if (previous != null) {
				throw new IllegalStateException("Bundled assistant definition " + file
						+ " repeats key '" + definition.key() + "' from " + previous);
			}
			definitions.add(definition);
		}
		for (AssistantDefinition definition : definitions) {
			try {
				validator.validate(definition, definitions);
			} catch (InvalidAssistantDefinitionException e) {
				throw new IllegalStateException("Bundled assistant definition "
						+ fileByKey.get(definition.key()) + " is invalid: "
						+ String.join("; ", e.problems()), e);
			}
		}
		return List.copyOf(definitions);
	}

	static AssistantDefinition parse(ObjectMapper objectMapper, Resource resource, String file) {
		try (InputStream in = resource.getInputStream()) {
			return fromJson(objectMapper.readTree(in));
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Bundled assistant definition " + file
					+ " could not be read: " + e.getMessage(), e);
		}
	}

	static AssistantDefinition fromJson(JsonNode node) {
		Set<String> scope = new LinkedHashSet<String>();
		node.path("scope").forEach(n -> scope.add(n.asText()));
		List<String> providers = new ArrayList<String>();
		node.path("contextProviders").forEach(n -> providers.add(n.asText()));
		List<VocabularyEntry> vocabulary = new ArrayList<VocabularyEntry>();
		node.path("vocabulary").forEach(n -> vocabulary.add(new VocabularyEntry(
				text(n, "type"), text(n, "description"))));
		java.util.Map<String, Integer> budgets = new java.util.LinkedHashMap<String, Integer>();
		node.path("contextBudgets").fields()
				.forEachRemaining(e -> budgets.put(e.getKey(), e.getValue().asInt()));
		String kind = text(node, "kind");
		return new AssistantDefinition(text(node, "key"), text(node, "displayName"),
				kind == null ? DefinitionKind.REVIEW : DefinitionKind.valueOf(kind),
				text(node, "taskType"), scope, providers, text(node, "instructions"), vocabulary,
				text(node, "outputSchemaName"), text(node, "outputSchemaVersion"),
				node.path("enabled").asBoolean(true), node.path("version").asInt(0),
				DefinitionSource.BUNDLED, null, null, text(node, "executorBean"), budgets);
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}
}
