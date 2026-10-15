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
package com.rreganjr.requel.service.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;

/**
 * Input for creating one of a project's own assistant definitions (issue #264). Needs
 * AssistantDefinition[Edit].
 *
 * @param projectName the project
 * @param kind REVIEW, POLICY or CORPUS; the task type and output schema follow from it
 * @param key 3 to 80 lowercase letters, digits or hyphens; not a bundled or built-in key
 * @param displayName the name its switch and its issues show
 * @param scope entity types (set kinds for CORPUS); empty is the fallback (every type for a policy)
 * @param contextProviders the context it reads, e.g. {@code entity}
 * @param contextBudgets per-provider character shares, optional
 * @param instructions the guidance sent to the model
 * @param vocabulary the finding types it may report
 * @param localOnly never send its work to a remote provider
 * @param executorBean never accepted from a project; present so naming one is refused
 */
@CommandDescription("Creates one of the project's own AI assistant definitions: a review, a"
        + " policy or a corpus analysis, with its instructions, scope and finding vocabulary.")
public record CreateAssistantDefinitionInput(
        @NotBlank String projectName,
        @NotBlank String kind,
        @NotBlank String key,
        String displayName,
        Set<String> scope,
        List<String> contextProviders,
        Map<String, Integer> contextBudgets,
        String instructions,
        List<AssistantDefinitionVocabularyInput> vocabulary,
        boolean localOnly,
        String executorBean
) {
}
