/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.service.api.dto.AssistantDefinitionVocabularyInput;
import com.rreganjr.requel.service.api.dto.ProjectDefinitionDto;

/** Issue #264: an assistant definition to and from the API's shapes. */
public final class AssistantDefinitionDtos {

	private AssistantDefinitionDtos() {
	}

	/** {@code view} for the API, with the project's switch for it ({@code null}: not read). */
	public static ProjectDefinitionDto toDto(ProjectAssistantDefinitions.View view,
			Boolean enabled) {
		if (view == null) {
			return null;
		}
		List<AssistantDefinitionVocabularyInput> vocabulary = new ArrayList<>();
		for (ProjectAssistantDefinitions.Vocabulary entry : view.vocabulary()) {
			vocabulary.add(new AssistantDefinitionVocabularyInput(entry.type(),
					entry.description(), entry.category()));
		}
		return new ProjectDefinitionDto(view.key(), view.displayName(), view.kind(),
				view.taskType(), view.scope(), view.contextProviders(), view.contextBudgets(),
				view.instructions(), vocabulary, view.localOnly(), view.source(), view.version(),
				view.forkedFromVersion(), view.bundledVersion(), view.lockVersion(),
				view.inEffectFor(), enabled);
	}

	/** The project author's definition as the store takes it. */
	public static ProjectAssistantDefinitions.Draft toDraft(String kind, String key,
			String displayName, Set<String> scope, List<String> contextProviders,
			java.util.Map<String, Integer> contextBudgets, String instructions,
			List<AssistantDefinitionVocabularyInput> vocabulary, boolean localOnly,
			String executorBean) {
		List<ProjectAssistantDefinitions.Vocabulary> entries = new ArrayList<>();
		if (vocabulary != null) {
			for (AssistantDefinitionVocabularyInput entry : vocabulary) {
				entries.add(entry == null ? null
						: new ProjectAssistantDefinitions.Vocabulary(entry.type(),
								entry.description(), entry.category()));
			}
		}
		return new ProjectAssistantDefinitions.Draft(kind, key, displayName, scope,
				contextProviders, contextBudgets, instructions, entries, localOnly, executorBean);
	}
}
