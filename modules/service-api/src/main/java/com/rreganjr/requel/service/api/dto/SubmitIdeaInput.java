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
import jakarta.validation.constraints.Size;

/**
 * Input for submitting a requirement idea to a project.
 *
 * @param projectName the project the idea is for; must exist
 * @param text        the idea in the user's words; at least 10 characters unless a text file is
 *                    attached
 */
@CommandDescription(value = "Submits a rough requirement idea to an existing project, as text and"
        + " optionally an attached .txt or .md file (up to 10 MB; its text is appended to the typed"
        + " text). Returns the saved idea with status SUBMITTED.",
        authorization = "Project[Edit]")
public record SubmitIdeaInput(
        @NotBlank @Size(max = 255) String projectName,
        @Size(max = 1000000) String text
) {
}
