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
import jakarta.validation.constraints.NotNull;

/**
 * Input for editing one of a project's assistant definitions: its own, or its copy of a bundled
 * one (issue #264). Needs AssistantDefinition[Edit].
 *
 * @param projectName the project
 * @param key the definition
 * @param version the lockVersion read; a stale one is refused
 * @param kind optional; a definition's kind can't change
 * @param executorBean never accepted from a project; present so naming one is refused
 */
@CommandDescription("Edits one of the project's AI assistant definitions: its name, scope,"
        + " context, instructions, finding vocabulary or local-only setting.")
public record EditAssistantDefinitionInput(
        @NotBlank String projectName,
        @NotBlank String key,
        @NotNull Integer version,
        String kind,
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
