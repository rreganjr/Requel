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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Issue #260: the rules an assistant definition must meet to be saved or seeded.
 *
 * <ul>
 * <li>a key, display name, task type and instructions;</li>
 * <li>kind {@code REVIEW} ({@code POLICY} waits for #265);</li>
 * <li>scope names only reviewable entity types;</li>
 * <li>context providers are known (until #261, only {@code entity});</li>
 * <li>a non-empty vocabulary with no repeated type;</li>
 * <li>an output schema from the allowed set;</li>
 * <li>instructions within the input budget ({@code requel.ai.max-input-tokens} x 4 characters);</li>
 * <li>no collision: per task type, at most one fallback (empty scope) and at most one specific
 * definition per entity type, among the definitions it would sit beside.</li>
 * </ul>
 */
public class AssistantDefinitionValidator {

	/** Entity types a review can target ({@code AiReviewService.REVIEWABLE_TYPES}). */
	public static final Set<String> REVIEWABLE_TYPES = Set.of("Goal", "Story", "Actor", "UseCase",
			"Scenario", "Step");

	/** Context packs a definition may name until #261 adds a provider registry. */
	public static final Set<String> CONTEXT_PROVIDERS = Set.of("entity");

	/** {@code name:version} of the output schemas the executor can load. */
	public static final Set<String> OUTPUT_SCHEMAS = Set.of("RequirementsReviewOutput:1");

	/** Characters per token used to turn the token budget into a character cap. */
	static final int CHARS_PER_TOKEN = 4;

	private final int maxInstructionChars;

	public AssistantDefinitionValidator(int maxInputTokens) {
		this.maxInstructionChars = Math.max(1, maxInputTokens) * CHARS_PER_TOKEN;
	}

	/**
	 * @param definition the definition to check
	 * @param neighbours the definitions it would run beside (same project's and bundled); the one it
	 *        replaces (same key and owner) is ignored
	 * @throws InvalidAssistantDefinitionException listing every problem found
	 */
	public void validate(AssistantDefinition definition, Collection<AssistantDefinition> neighbours) {
		Objects.requireNonNull(definition, "definition");
		List<String> problems = new ArrayList<String>();
		if (blank(definition.key())) {
			problems.add("key is required");
		}
		if (blank(definition.displayName())) {
			problems.add("displayName is required");
		}
		if (blank(definition.taskType())) {
			problems.add("taskType is required");
		}
		if (definition.kind() != DefinitionKind.REVIEW) {
			problems.add("kind " + definition.kind() + " is not supported yet; only REVIEW");
		}
		Set<String> unknownTypes = new TreeSet<String>(definition.scope());
		unknownTypes.removeAll(REVIEWABLE_TYPES);
		if (!unknownTypes.isEmpty()) {
			problems.add("unknown entity type(s) in scope: " + unknownTypes + " (known: "
					+ new TreeSet<String>(REVIEWABLE_TYPES) + ")");
		}
		if (definition.contextProviders().isEmpty()) {
			problems.add("at least one context provider is required");
		}
		Set<String> unknownProviders = new TreeSet<String>(definition.contextProviders());
		unknownProviders.removeAll(CONTEXT_PROVIDERS);
		if (!unknownProviders.isEmpty()) {
			problems.add("unknown context provider(s): " + unknownProviders + " (known: "
					+ new TreeSet<String>(CONTEXT_PROVIDERS) + ")");
		}
		checkVocabulary(definition, problems);
		if (!OUTPUT_SCHEMAS.contains(definition.outputSchemaName() + ":"
				+ definition.outputSchemaVersion())) {
			problems.add("output schema " + definition.outputSchemaName() + " v"
					+ definition.outputSchemaVersion() + " is not in the allowed set "
					+ new TreeSet<String>(OUTPUT_SCHEMAS));
		}
		if (blank(definition.instructions())) {
			problems.add("instructions are required");
		} else if (definition.instructions().length() > maxInstructionChars) {
			problems.add("instructions are " + definition.instructions().length()
					+ " characters; the input budget allows " + maxInstructionChars);
		}
		if (definition.source() == DefinitionSource.PROJECT && definition.projectId() == null) {
			problems.add("a PROJECT definition needs an owning project");
		}
		if (definition.source() == DefinitionSource.BUNDLED && definition.projectId() != null) {
			problems.add("a BUNDLED definition has no owning project");
		}
		checkCollisions(definition, neighbours, problems);
		if (!problems.isEmpty()) {
			throw new InvalidAssistantDefinitionException(definition.key(), problems);
		}
	}

	private static void checkVocabulary(AssistantDefinition definition, List<String> problems) {
		if (definition.vocabulary().isEmpty()) {
			problems.add("the finding vocabulary is empty");
			return;
		}
		Set<String> seen = new HashSet<String>();
		for (VocabularyEntry entry : definition.vocabulary()) {
			if (entry == null || blank(entry.type())) {
				problems.add("a vocabulary entry has no type");
			} else if (!seen.add(entry.type())) {
				problems.add("vocabulary type " + entry.type() + " is listed twice");
			}
		}
	}

	private static void checkCollisions(AssistantDefinition definition,
			Collection<AssistantDefinition> neighbours, List<String> problems) {
		if (neighbours == null) {
			return;
		}
		for (AssistantDefinition other : neighbours) {
			if (other == null || replaces(definition, other)
					|| !Objects.equals(definition.taskType(), other.taskType())) {
				continue;
			}
			if (definition.isFallback() && other.isFallback()) {
				problems.add("task " + definition.taskType() + " already has a fallback definition ("
						+ other.key() + "); only one definition may have an empty scope");
			} else if (!definition.isFallback() && !other.isFallback()) {
				Set<String> shared = new TreeSet<String>(definition.scope());
				shared.retainAll(other.scope());
				if (!shared.isEmpty()) {
					problems.add("scope " + shared + " collides with definition " + other.key()
							+ " for task " + definition.taskType());
				}
			}
		}
	}

	/**
	 * The other definition is the one being replaced: the same key, and either the same owner or a
	 * bundled one this project definition overrides.
	 */
	private static boolean replaces(AssistantDefinition definition, AssistantDefinition other) {
		if (!Objects.equals(definition.key(), other.key())) {
			return false;
		}
		return Objects.equals(definition.projectId(), other.projectId())
				|| (definition.projectId() != null && other.projectId() == null);
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
