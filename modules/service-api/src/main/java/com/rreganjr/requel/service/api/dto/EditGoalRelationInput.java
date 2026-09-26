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

/**
 * Input DTO for creating or editing a goal relation.
 *
 * @param projectName    project context
 * @param fromGoalName   name of the origin goal
 * @param toGoalName     name of the target goal
 * @param relationType   "Supports" or "Conflicts"
 * @param version        optimistic lock version (null for create)
 */
@CommandDescription("Creates a directed relation from one goal to another, or changes the type of"
        + " an existing one. The goals are given by name, not id: fromGoalName and toGoalName."
        + " relationType is exactly Supports or Conflicts, and a goal cannot be related to itself."
        + " Without version a new relation is always created, so repeating an existing one fails;"
        + " with version the existing relation's type is updated, and the goal names must then"
        + " match exactly, including case. Nothing is returned: read the goal to see the relation"
        + " and its id.")
public record EditGoalRelationInput(
        @NotBlank String projectName,
        @NotBlank String fromGoalName,
        @NotBlank String toGoalName,
        @NotBlank String relationType,
        Integer version
) {
}
