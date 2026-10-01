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
import java.util.Set;

/** Test fixtures for assistant definitions. */
final class Definitions {

	static final String TASK = "REQUIREMENTS_REVIEW";

	private Definitions() {
	}

	static AssistantDefinition review(String key, Set<String> scope) {
		return new AssistantDefinition(key, "Review " + key, DefinitionKind.REVIEW, TASK, scope,
				List.of("entity"), "Review the entity.",
				List.of(new VocabularyEntry("AMBIGUOUS", "unclear")), "RequirementsReviewOutput",
				"1", true, 1, DefinitionSource.BUNDLED, null, null, null);
	}

	static AssistantDefinition fallback(String key) {
		return review(key, Set.of());
	}

	static AssistantDefinition project(AssistantDefinition definition, long projectId) {
		return new AssistantDefinition(definition.key(), definition.displayName(),
				definition.kind(), definition.taskType(), definition.scope(),
				definition.contextProviders(), definition.instructions(), definition.vocabulary(),
				definition.outputSchemaName(), definition.outputSchemaVersion(),
				definition.enabled(), definition.version(), DefinitionSource.PROJECT, projectId,
				null, definition.executorBean());
	}
}
