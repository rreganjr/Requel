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
package com.rreganjr.requel.service.api.dto;

import com.rreganjr.requel.service.api.CommandDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input DTO for EditPosition command. positionId null = create new position on issue.
 */
@CommandDescription("Creates or edits a position: a proposed answer to the issue given by"
        + " issueId. Leave positionId null to create one; when a position with the same text"
        + " already exists on any issue in the project, that position is linked to this issue"
        + " instead, and any duplicates of that text are first merged into the one with the lowest"
        + " id. Pass positionId to replace a position's text. A position can answer several issues,"
        + " so the change shows on all of them, and text that another position in the project"
        + " already has is refused, naming that position's id so you can edit it instead. An"
        + " unknown positionId creates a new position rather than failing.")
public record EditPositionInput(
        String projectName,
        @NotNull Long issueId,
        Long positionId,
        @NotBlank String text
) {
}
