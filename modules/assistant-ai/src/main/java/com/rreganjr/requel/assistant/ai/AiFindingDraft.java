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
import java.util.Map;
import java.util.Objects;

/**
 * Draft finding returned by an AI provider. These drafts are converted to
 * reviewable annotation actions by an assistant, never executed directly.
 */
public record AiFindingDraft(String findingType, String severity, Double confidence,
		List<String> evidenceReferences, String suggestedIssueText, String suggestedNoteText,
		List<String> suggestedPositions, Map<String, Object> metadata) {

	public AiFindingDraft {
		Objects.requireNonNull(findingType, "findingType");
		evidenceReferences = List.copyOf(Objects.requireNonNull(evidenceReferences,
				"evidenceReferences"));
		suggestedPositions = List.copyOf(Objects.requireNonNull(suggestedPositions,
				"suggestedPositions"));
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
	}
}
