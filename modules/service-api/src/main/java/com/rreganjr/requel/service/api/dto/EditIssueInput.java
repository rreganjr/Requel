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
import jakarta.validation.constraints.Pattern;

/**
 * Input DTO for EditIssue command. issueId null = create new issue on entity.
 *
 * <p>Partial update (#271, following #316): on an update ({@code issueId} set) a null
 * {@code mustBeResolved} or {@code severity} leaves that property unchanged. On create a null
 * {@code mustBeResolved} is {@code false} and a null {@code severity} is {@code MEDIUM}.
 * {@code severity} is case-insensitive; anything outside {@code LOW | MEDIUM | HIGH} is a
 * field-level validation error on {@code severity}.
 */
@CommandDescription("Creates or edits an issue attached to the entity given by entityType and"
        + " entityId. entityType is case-sensitive: Project, ProjectTeam, Goal, GoalRelation,"
        + " UseCase, Scenario, Step, Story, Actor, GlossaryTerm, NonUserStakeholder or"
        + " UserStakeholder. Leave issueId null to create one; when the entity already has an issue"
        + " with exactly the same text, that issue is reused and only a supplied severity is"
        + " applied to it. severity is LOW, MEDIUM or HIGH, ignoring case, and cannot be cleared; a"
        + " new issue gets MEDIUM and mustBeResolved false. When editing, a null mustBeResolved or"
        + " severity keeps its current value. Editing an issue attached elsewhere also attaches it"
        + " to this entity, and an unknown issueId creates a new issue rather than failing. Issues"
        + " are resolved in the Requel UI, not through the gateway.")
public record EditIssueInput(
        String projectName,
        @NotBlank String entityType,
        @NotNull Long entityId,
        Long issueId,
        @NotBlank String text,
        Boolean mustBeResolved,
        @Pattern(regexp = "\\s*(LOW|MEDIUM|HIGH)\\s*", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of LOW, MEDIUM, HIGH")
        String severity
) {
}
