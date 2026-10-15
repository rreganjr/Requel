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
 * Input DTO for EditNote command. noteId null = create new note on entity.
 */
@CommandDescription("Creates or edits a note attached to the entity given by entityType and"
        + " entityId. entityType is case-sensitive: Project, ProjectTeam, Goal, GoalRelation,"
        + " UseCase, Scenario, Step, Story, Actor, GlossaryTerm, NonUserStakeholder or"
        + " UserStakeholder. Leave noteId null to create a note; when a note with the same text,"
        + " ignoring case, already exists anywhere in the project, that note is attached instead of"
        + " a new one. Pass noteId to replace a note's text. A note can be attached to several"
        + " entities, so editing it changes it everywhere, and an unknown noteId creates a new note"
        + " rather than failing.")
public record EditNoteInput(
        String projectName,
        @NotBlank String entityType,
        @NotNull Long entityId,
        Long noteId,
        @NotBlank String text
) {
}
