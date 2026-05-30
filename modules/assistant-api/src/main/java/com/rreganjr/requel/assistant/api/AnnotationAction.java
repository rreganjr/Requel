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
package com.rreganjr.requel.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Draft annotation mutation produced by an assistant and applied by core code
 * through existing annotation commands.
 *
 * <p>
 * The {@link ActionType} enum is intentionally expressed at the granularity of
 * <em>(operation x annotation kind)</em> so the core applicator can map each
 * action to the correct {@code AnnotationCommandFactory} method without the
 * assistant naming a command. The applicator follows the "Edit" convention: a
 * {@code CREATE_OR_UPDATE_*} action creates when no existing annotation matches
 * the idempotency key and updates (by id + optimistic-lock version) when one
 * does. See the command-mapping table in {@code doc/assistant-spi-plan.md}.
 *
 * <p>
 * Sub-variants that share a kind but resolve to different factory methods - a
 * lexical issue vs a general issue, or a "change spelling" / "add word to
 * dictionary" position vs a general position - are <strong>not</strong> separate
 * action types. They are selected by the applicator from {@link #metadata}, e.g.
 * {@code metadata.get("kind")} = {@code "LEXICAL"}, {@code "CHANGE_SPELLING"}, or
 * {@code "ADD_WORD_TO_DICTIONARY"}. This keeps the enum small and stable while
 * still covering every factory variant.
 */
public record AnnotationAction(String actionKey, ActionType actionType, EntityRef targetRef,
		String parentActionKey, String text, String severity, Double confidence,
		List<EvidenceRef> evidence, Map<String, Object> metadata) {

	/**
	 * Operation x annotation-kind. {@code CREATE_OR_UPDATE_*} maps to the
	 * factory's {@code newEdit*Command()} (create or update via id presence).
	 */
	public enum ActionType {
		CREATE_OR_UPDATE_NOTE,
		DELETE_NOTE,
		CREATE_OR_UPDATE_ISSUE,
		RESOLVE_ISSUE,
		DELETE_ISSUE,
		CREATE_OR_UPDATE_POSITION,
		DELETE_POSITION,
		CREATE_OR_UPDATE_ARGUMENT,
		DELETE_ARGUMENT,
		/** Detach an annotation from one annotatable while keeping it elsewhere. */
		REMOVE_ANNOTATION_FROM_ANNOTATABLE
	}

	public AnnotationAction {
		Objects.requireNonNull(actionKey, "actionKey");
		Objects.requireNonNull(actionType, "actionType");
		if (targetRef == null && parentActionKey == null) {
			throw new IllegalArgumentException(
					"AnnotationAction requires a targetRef or a parentActionKey: " + actionKey);
		}
		evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
	}

	public Optional<String> textValue() {
		return Optional.ofNullable(text);
	}

	/**
	 * Action key of another action in the same {@link AssistantResult} this
	 * action attaches to (e.g. a position whose parent issue was created in the
	 * same run and therefore has no persisted id yet). The applicator resolves
	 * the parent's freshly-created annotation id before applying this action.
	 */
	public Optional<String> parentActionKeyValue() {
		return Optional.ofNullable(parentActionKey);
	}

	// ---- Convenience factories -------------------------------------------------

	public static AnnotationAction createNote(String actionKey, EntityRef targetRef, String text,
			List<EvidenceRef> evidence) {
		return new AnnotationAction(actionKey, ActionType.CREATE_OR_UPDATE_NOTE, targetRef, null,
				text, null, null, evidence, Map.of());
	}

	public static AnnotationAction createIssue(String actionKey, EntityRef targetRef, String text,
			boolean mustResolve, List<EvidenceRef> evidence) {
		return new AnnotationAction(actionKey, ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				text, null, null, evidence, Map.of("mustResolve", mustResolve));
	}

	/** Position attached to an already-persisted issue referenced by {@code issueRef}. */
	public static AnnotationAction createPosition(String actionKey, EntityRef issueRef, String text,
			List<EvidenceRef> evidence) {
		return new AnnotationAction(actionKey, ActionType.CREATE_OR_UPDATE_POSITION, issueRef, null,
				text, null, null, evidence, Map.of());
	}

	/**
	 * Position attached to an issue created earlier in the same result and
	 * referenced by its {@code parentActionKey} (the issue has no persisted id
	 * yet). The applicator resolves the parent id before applying.
	 */
	public static AnnotationAction createPositionForDraftIssue(String actionKey,
			String parentIssueActionKey, String text, List<EvidenceRef> evidence) {
		return new AnnotationAction(actionKey, ActionType.CREATE_OR_UPDATE_POSITION, null,
				parentIssueActionKey, text, null, null, evidence, Map.of());
	}
}
