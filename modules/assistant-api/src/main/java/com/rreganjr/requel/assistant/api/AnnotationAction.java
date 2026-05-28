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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Draft annotation mutation produced by an assistant and applied by core code
 * through existing commands.
 */
public record AnnotationAction(String actionKey, ActionType actionType, EntityRef targetRef,
		String text, String severity, Double confidence, Map<String, Object> metadata) {

	public enum ActionType {
		CREATE_NOTE,
		CREATE_ISSUE,
		CREATE_POSITION,
		CREATE_ARGUMENT,
		UPDATE_TEXT,
		RESOLVE_ISSUE,
		REMOVE_ANNOTATION
	}

	public AnnotationAction {
		Objects.requireNonNull(actionKey, "actionKey");
		Objects.requireNonNull(actionType, "actionType");
		Objects.requireNonNull(targetRef, "targetRef");
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
	}

	public Optional<String> textValue() {
		return Optional.ofNullable(text);
	}

	public static AnnotationAction createIssue(String actionKey, EntityRef targetRef,
			String text, boolean mustResolve) {
		return new AnnotationAction(actionKey, ActionType.CREATE_ISSUE, targetRef, text, null,
				null, Map.of("mustResolve", mustResolve));
	}

	public static AnnotationAction createNote(String actionKey, EntityRef targetRef,
			String text) {
		return new AnnotationAction(actionKey, ActionType.CREATE_NOTE, targetRef, text, null,
				null, Map.of());
	}

	public static AnnotationAction createPosition(String actionKey, EntityRef issueRef,
			String text) {
		return new AnnotationAction(actionKey, ActionType.CREATE_POSITION, issueRef, text, null,
				null, Map.of());
	}
}
