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
package com.rreganjr.requel.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Issue #260: the assistant definitions a project owns, as far as the project side needs to know
 * them: deleting a project deletes them. Implemented in assistant-core.
 *
 * <p>Issue #264: and authoring them. A project can fork a bundled definition (a project row with
 * the bundled key, which overrides it there), edit that copy, revert it (the row goes, so the
 * bundled definition is back exactly), and create and delete definitions of its own. Every write
 * is validated like a bundled definition, plus the project-only rules; a broken rule throws
 * {@link InvalidDefinitionException} naming the field, and a stale {@code lockVersion} throws
 * {@code EntityLockException}. Callers check the {@code AssistantDefinition[Edit]} permission.
 */
public interface ProjectAssistantDefinitions {

	/** Delete every definition {@code projectId} owns; returns how many. */
	int deleteForProject(Long projectId);

	/** A finding type a definition may report. */
	record Vocabulary(String type, String description, String category) {
	}

	/**
	 * What a project author writes. {@code kind} (REVIEW, POLICY or CORPUS) is read only on
	 * create; the task type and output schema follow from it. {@code executorBean} is here only so
	 * a write naming one is refused with a field error rather than silently dropped.
	 */
	record Draft(String kind, String key, String displayName, Set<String> scope,
			List<String> contextProviders, Map<String, Integer> contextBudgets,
			String instructions, List<Vocabulary> vocabulary, boolean localOnly,
			String executorBean) {

		/**
		 * Copies, so the draft can't change under a write. A null name in the scope or the context
		 * is dropped; a null vocabulary entry is kept, so validation reports it.
		 */
		public Draft {
			scope = scope == null ? Set.of()
					: Set.copyOf(scope.stream().filter(java.util.Objects::nonNull).toList());
			contextProviders = contextProviders == null ? List.of()
					: contextProviders.stream().filter(java.util.Objects::nonNull).toList();
			contextBudgets = contextBudgets == null ? Map.of()
					: java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(
							contextBudgets));
			vocabulary = vocabulary == null ? List.of()
					: java.util.Collections.unmodifiableList(new java.util.ArrayList<>(vocabulary));
		}
	}

	/**
	 * A definition as a project sees it.
	 *
	 * @param source BUNDLED or PROJECT
	 * @param version the content version (bumped by every saved edit; recorded on each run)
	 * @param forkedFromVersion the bundled version a fork was made from, or null
	 * @param bundledVersion the current bundled version of the key, or null for a project-only key;
	 *        above {@code forkedFromVersion} means the bundled definition has moved on
	 * @param lockVersion the optimistic-lock version a write must send back (0 for bundled)
	 * @param inEffectFor the entity types (or set kinds) this definition covers in the project,
	 *        after a specific definition beats the fallback; every type for a policy with an empty
	 *        scope
	 */
	record View(String key, String displayName, String kind, String taskType, Set<String> scope,
			List<String> contextProviders, Map<String, Integer> contextBudgets,
			String instructions, List<Vocabulary> vocabulary, boolean localOnly, String source,
			int version, Integer forkedFromVersion, Integer bundledVersion, int lockVersion,
			List<String> inEffectFor) {
	}

	/** Every definition in effect in {@code projectId}: bundled, overridden by its own. */
	default List<View> effective(Long projectId) {
		return List.of();
	}

	/** The definition {@code key} as {@code projectId} sees it, or empty. */
	default Optional<View> find(Long projectId, String key) {
		return Optional.empty();
	}

	/** The bundled definition {@code key} (the baseline a fork came from), or empty. */
	default Optional<View> bundled(String key) {
		return Optional.empty();
	}

	/** A new project-only definition. */
	default View create(Long projectId, Draft draft, String by) {
		throw new UnsupportedOperationException("authoring is not available");
	}

	/** Change the project's own definition {@code key}. */
	default View edit(Long projectId, String key, int lockVersion, Draft draft, String by) {
		throw new UnsupportedOperationException("authoring is not available");
	}

	/** Copy the bundled definition {@code key} into the project, to edit. */
	default View fork(Long projectId, String key, String by) {
		throw new UnsupportedOperationException("authoring is not available");
	}

	/** Drop the project's copy of a bundled definition: the bundled one is back exactly. */
	default void revert(Long projectId, String key, int lockVersion) {
		throw new UnsupportedOperationException("authoring is not available");
	}

	/** Delete a definition the project created. */
	default void delete(Long projectId, String key, int lockVersion) {
		throw new UnsupportedOperationException("authoring is not available");
	}
}
