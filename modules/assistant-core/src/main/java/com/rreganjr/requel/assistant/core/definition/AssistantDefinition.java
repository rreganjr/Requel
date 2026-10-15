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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Issue #260: an assistant described as data - what it reviews, with which context, under which
 * instructions, reporting which finding types in which output schema. One generic executor runs
 * any {@link DefinitionKind#REVIEW} definition; {@code executorBean} names a bean to run instead.
 *
 * @param key stable identifier, also the assistant id of the executor that runs it (and so part
 *        of every finding's idempotency key)
 * @param displayName shown on the project overview's assistant toggles
 * @param kind REVIEW, POLICY or CORPUS
 * @param taskType the run task it serves, e.g. {@code REQUIREMENTS_REVIEW}
 * @param scope entity type names it applies to; empty makes it the fallback for its task type.
 *        For a CORPUS definition, the set kinds it analyzes (PROJECT, GOAL, USE_CASE; #266)
 * @param contextProviders the context packs it reads (until #261, only {@code entity})
 * @param instructions the task guidance sent to the provider
 * @param vocabulary the finding types it may report
 * @param outputSchemaName output schema name, from the allowed set
 * @param outputSchemaVersion output schema version
 * @param enabled the install-wide switch
 * @param version bumped on every change; a seed replaces a bundled row only with a higher one
 * @param source BUNDLED or PROJECT
 * @param projectId the owning project of a PROJECT definition; null for BUNDLED
 * @param forkedFromVersion the bundled version a project copy was made from, or null
 * @param executorBean a bean name to run instead of the generic executor, or null
 * @param contextBudgets per-provider character shares overriding the default (#261)
 * @param localOnly never send this definition's work to a remote provider (#265)
 */
public record AssistantDefinition(String key, String displayName, DefinitionKind kind,
		String taskType, Set<String> scope, List<String> contextProviders, String instructions,
		List<VocabularyEntry> vocabulary, String outputSchemaName, String outputSchemaVersion,
		boolean enabled, int version, DefinitionSource source, Long projectId,
		Integer forkedFromVersion, String executorBean, Map<String, Integer> contextBudgets,
		boolean localOnly) {

	/** #265: the task type policy definitions serve, beside {@code REQUIREMENTS_REVIEW}. */
	public static final String POLICY_REVIEW = "POLICY_REVIEW";

	/** #265: the output schema a policy definition uses (the review finding plus a policy key). */
	public static final String POLICY_OUTPUT_SCHEMA = "PolicyReviewOutput";

	/** #266: the task type corpus definitions serve, dispatched only by an explicit request. */
	public static final String CORPUS_REVIEW = "CORPUS_REVIEW";

	/** #266: the output schema a corpus definition uses (a finding names its participants). */
	public static final String CORPUS_OUTPUT_SCHEMA = "CorpusReviewOutput";

	/** #266: the set kinds a corpus definition's scope may name. */
	public static final Set<String> CORPUS_SET_KINDS = Set.of("PROJECT", "GOAL", "USE_CASE");

	/** Not local-only (#265). */
	public AssistantDefinition(String key, String displayName, DefinitionKind kind,
			String taskType, Set<String> scope, List<String> contextProviders, String instructions,
			List<VocabularyEntry> vocabulary, String outputSchemaName, String outputSchemaVersion,
			boolean enabled, int version, DefinitionSource source, Long projectId,
			Integer forkedFromVersion, String executorBean, Map<String, Integer> contextBudgets) {
		this(key, displayName, kind, taskType, scope, contextProviders, instructions, vocabulary,
				outputSchemaName, outputSchemaVersion, enabled, version, source, projectId,
				forkedFromVersion, executorBean, contextBudgets, false);
	}

	/** Without context budget overrides (every provider gets the default share). */
	public AssistantDefinition(String key, String displayName, DefinitionKind kind,
			String taskType, Set<String> scope, List<String> contextProviders, String instructions,
			List<VocabularyEntry> vocabulary, String outputSchemaName, String outputSchemaVersion,
			boolean enabled, int version, DefinitionSource source, Long projectId,
			Integer forkedFromVersion, String executorBean) {
		this(key, displayName, kind, taskType, scope, contextProviders, instructions, vocabulary,
				outputSchemaName, outputSchemaVersion, enabled, version, source, projectId,
				forkedFromVersion, executorBean, Map.of());
	}

	public AssistantDefinition {
		contextBudgets = contextBudgets == null ? Map.of() : Map.copyOf(contextBudgets);
		scope = scope == null ? Set.of() : Set.copyOf(scope);
		contextProviders = contextProviders == null ? List.of() : List.copyOf(contextProviders);
		vocabulary = vocabulary == null ? List.of() : List.copyOf(vocabulary);
		source = source == null ? DefinitionSource.BUNDLED : source;
		kind = kind == null ? DefinitionKind.REVIEW : kind;
	}

	/** A cross-cutting policy (#265): composed with the other policies into one call. */
	public boolean isPolicy() {
		return kind == DefinitionKind.POLICY;
	}

	/** A corpus analysis (#266): findings about relationships between a set's entities. */
	public boolean isCorpus() {
		return kind == DefinitionKind.CORPUS;
	}

	/**
	 * True when this definition runs for {@code entityType}: a policy with an empty scope runs
	 * for every type (#265); a review's empty scope makes it the fallback instead.
	 */
	public boolean appliesTo(String entityType) {
		return scope.contains(entityType) || (isPolicy() && scope.isEmpty());
	}

	/** An empty scope: runs for any entity type no specific definition covers. */
	public boolean isFallback() {
		return scope.isEmpty();
	}

	/** True when this definition's scope names {@code entityType} (a fallback names none). */
	public boolean isSpecificTo(String entityType) {
		return scope.contains(entityType);
	}

	/** The same definition with a different install-wide switch. */
	public AssistantDefinition withEnabled(boolean value) {
		return new AssistantDefinition(key, displayName, kind, taskType, scope, contextProviders,
				instructions, vocabulary, outputSchemaName, outputSchemaVersion, value, version,
				source, projectId, forkedFromVersion, executorBean, contextBudgets, localOnly);
	}

	/** A project copy of this definition, for {@code projectId}, at version 1. */
	public AssistantDefinition forkFor(Long owner, String newInstructions) {
		Objects.requireNonNull(owner, "owner");
		return new AssistantDefinition(key, displayName, kind, taskType, scope, contextProviders,
				newInstructions, vocabulary, outputSchemaName, outputSchemaVersion, enabled, 1,
				DefinitionSource.PROJECT, owner, version, executorBean, contextBudgets, localOnly);
	}

	/** {@code key@version}, for the run record. */
	public String label() {
		return key + "@" + version;
	}
}
