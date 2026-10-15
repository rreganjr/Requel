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

import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;

/**
 * Input for switching one of a project's assistants on or off (issue #268).
 *
 * @param projectName the project
 * @param assistantId an assistant id from GET /api/projects/{name}/assistants
 * @param enabled whether the assistant runs in the project
 */
@CommandDescription("Switches one of the project's assistants (spelling, vague words, glossary"
        + " candidates, complex sentences) on or off. A switched-off assistant stops running in"
        + " the project; the issues it already raised stay as they are.")
public record EditProjectAssistantSettingInput(
        @NotBlank String projectName,
        @NotBlank String assistantId,
        boolean enabled
) {
}
