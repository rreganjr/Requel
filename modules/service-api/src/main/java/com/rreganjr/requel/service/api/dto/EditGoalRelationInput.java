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

import jakarta.validation.constraints.NotBlank;

/**
 * Input DTO for creating or editing a goal relation.
 *
 * @param projectName    project context
 * @param fromGoalName   name of the origin goal
 * @param toGoalName     name of the target goal
 * @param relationType   "Supports" or "Conflicts"
 * @param version        optimistic lock version (null for create)
 */
public record EditGoalRelationInput(
        @NotBlank String projectName,
        @NotBlank String fromGoalName,
        @NotBlank String toGoalName,
        @NotBlank String relationType,
        Integer version
) {
}
