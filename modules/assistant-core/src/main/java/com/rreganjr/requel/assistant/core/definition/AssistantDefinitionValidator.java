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

import com.rreganjr.requel.assistant.core.context.ContextProviderRegistry;
/**
 * Issue #260: the rules an assistant definition must meet to be saved or seeded.
 *
 * <ul>
 * <li>a key, display name, task type and instructions;</li>
 * <li>kind {@code REVIEW}, or {@code POLICY} with task {@code POLICY_REVIEW} and the
 * {@code PolicyReviewOutput} schema (#265); a review can't serve {@code POLICY_REVIEW};</li>
 * <li>scope names only reviewable entity types;</li>
 * <li>context providers are known (until #261, only {@code entity});</li>
 * <li>a non-empty vocabulary with no repeated type;</li>
 * <li>an output schema from the allowed set;</li>
 * <li>instructions within the input budget ({@code requel.ai.max-input-tokens} x 4 characters);</li>
 * <li>no collision: per task type, at most one fallback (empty scope) and at most one specific
 * definition per entity type, among the definitions it would sit beside. Policies are exempt:
 * many may apply to one entity, which is what composing them is for (#265).</li>
 * </ul>
 */
public class AssistantDefinitionValidator {

	/** Entity types a review can target ({@code AiReviewService.REVIEWABLE_TYPES}). */
	public static final Set<String> REVIEWABLE_TYPES = Set.of("Goal", "Story", "Actor", "UseCase",
			"Scenario", "Step", "GlossaryTerm");

	/**
	 * Issue #261: the built-in context provider ids, used when no {@code ContextProviderRegistry}
	 * is at hand (unit tests). The store validates against the registry's ids.
	 */
	public static final Set<String> CONTEXT_PROVIDERS = Set.of(ContextProviderRegistry.ENTITY,
			"goal-relations", "goal-siblings", "goal-stakeholders", "usecase-scenarios",
			"scenario-usecases", "step-sequence", "story-actors", "actor-references",
			"glossary-related", "project-names", "project-glossary");

	/** {@code name:version} of the output schemas the executor can load. */
	public static final Set<String> OUTPUT_SCHEMAS = Set.of("RequirementsReviewOutput:1",
			"RequirementsReviewOutput:2", AssistantDefinition.POLICY_OUTPUT_SCHEMA + ":1",
			AssistantDefinition.CORPUS_OUTPUT_SCHEMA + ":1");

	/**
	 * Issue #266: the context a corpus definition reads. The corpus executor builds both; they are
	 * not per-entity providers, so only a corpus definition may name them.
	 */
	public static final Set<String> CORPUS_PROVIDERS = Set.of("corpus-index", "corpus-candidates");

	/** Characters per token used to turn the token budget into a character cap. */
	static final int CHARS_PER_TOKEN = 4;

	private final int maxInstructionChars;
	private final Set<String> contextProviders;

	public AssistantDefinitionValidator(int maxInputTokens) {
		this(maxInputTokens, CONTEXT_PROVIDERS);
	}

	/** @param contextProviders the provider ids a definition may name (#261) */
	public AssistantDefinitionValidator(int maxInputTokens, Set<String> contextProviders) {
		this.maxInstructionChars = Math.max(1, maxInputTokens) * CHARS_PER_TOKEN;
		this.contextProviders = Set.copyOf(contextProviders);
	}

	/**
	 * @param definition the definition to check
	 * @param neighbours the definitions it would run beside (same project's and bundled); the one it
	 *        replaces (same key and owner) is ignored
	 * @throws InvalidAssistantDefinitionException listing every problem found
	 */
	/**
	 * Issue #261: a budget override names a provider the definition uses, is positive, and the
	 * overrides together fit the input cap.
	 */
	private void checkContextBudgets(AssistantDefinition definition, List<String> problems) {
		long total = 0;
		for (java.util.Map.Entry<String, Integer> budget : definition.contextBudgets().entrySet()) {
			if (!definition.contextProviders().contains(budget.getKey())
					|| ContextProviderRegistry.ENTITY.equals(budget.getKey())) {
				problems.add("contextBudgets names " + budget.getKey()
						+ ", which is not one of the definition's context providers");
			}
			if (budget.getValue() == null || budget.getValue() <= 0) {
				problems.add("contextBudgets." + budget.getKey() + " must be positive");
			} else {
				total += budget.getValue();
			}
		}
		if (total > maxInstructionChars) {
			problems.add("contextBudgets total " + total + " characters is over the input cap of "
					+ maxInstructionChars);
		}
	}

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
		checkKind(definition, problems);
		// #266: a corpus definition's scope is set kinds, and it reads the corpus context only
		Set<String> knownScope = definition.isCorpus() ? AssistantDefinition.CORPUS_SET_KINDS
				: REVIEWABLE_TYPES;
		Set<String> unknownTypes = new TreeSet<String>(definition.scope());
		unknownTypes.removeAll(knownScope);
		if (!unknownTypes.isEmpty()) {
			problems.add("unknown " + (definition.isCorpus() ? "set kind" : "entity type")
					+ "(s) in scope: " + unknownTypes + " (known: " + new TreeSet<String>(knownScope)
					+ ")");
		}
		if (definition.contextProviders().isEmpty()) {
			problems.add("at least one context provider is required");
		}
		Set<String> knownProviders = definition.isCorpus() ? CORPUS_PROVIDERS : contextProviders;
		Set<String> unknownProviders = new TreeSet<String>(definition.contextProviders());
		unknownProviders.removeAll(knownProviders);
		if (!unknownProviders.isEmpty()) {
			problems.add("unknown context provider(s): " + unknownProviders + " (known: "
					+ new TreeSet<String>(knownProviders) + ")");
		}
		checkContextBudgets(definition, problems);
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

	private static void checkKind(AssistantDefinition definition, List<String> problems) {
		checkPairing(definition, problems, DefinitionKind.POLICY, AssistantDefinition.POLICY_REVIEW,
				AssistantDefinition.POLICY_OUTPUT_SCHEMA);
		checkPairing(definition, problems, DefinitionKind.CORPUS, AssistantDefinition.CORPUS_REVIEW,
				AssistantDefinition.CORPUS_OUTPUT_SCHEMA);
	}

	/**
	 * A {@code kind} definition serves {@code task} with {@code schema}, and no other kind uses
	 * either (#265, #266).
	 */
	private static void checkPairing(AssistantDefinition definition, List<String> problems,
			DefinitionKind kind, String task, String schema) {
		boolean kindTask = task.equals(definition.taskType());
		boolean kindSchema = schema.equals(definition.outputSchemaName());
		if (definition.kind() == kind) {
			if (!blank(definition.taskType()) && !kindTask) {
				problems.add("a " + kind + " definition serves task " + task + ", not "
						+ definition.taskType());
			}
			if (!kindSchema) {
				problems.add("a " + kind + " definition uses the " + schema + " output schema");
			}
		} else {
			if (kindTask) {
				problems.add("task " + task + " is for " + kind + " definitions; this one is "
						+ definition.kind());
			}
			if (kindSchema) {
				problems.add("the " + schema + " output schema is for " + kind + " definitions");
			}
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
			if (entry != null && !VocabularyEntry.QUALITY.equals(entry.category())
					&& !VocabularyEntry.EXTRACTION.equals(entry.category())) {
				problems.add("vocabulary type " + entry.type() + " has unknown category "
						+ entry.category() + " (known: quality, extraction)");
			}
		}
	}

	private static void checkCollisions(AssistantDefinition definition,
			Collection<AssistantDefinition> neighbours, List<String> problems) {
		if (neighbours == null || definition.isPolicy()) {
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
