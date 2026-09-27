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

import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.service.api.AllowedValues;
import com.rreganjr.requel.service.api.CommandDescription;
import jakarta.validation.constraints.NotBlank;

/**
 * Input DTO for creating or editing a goal relation.
 *
 * @param projectName    project context
 * @param fromGoalName   name of the origin goal
 * @param toGoalName     name of the target goal
 * @param relationType   a {@code GoalRelationType} name, ignoring case; the schema lists them
 * @param version        optimistic lock version (null for create)
 */
@CommandDescription("Creates a directed relation from one goal to another, or changes the type of"
        + " an existing one. The goals are given by name, not id: fromGoalName and toGoalName."
        + " relationType is one of the values the schema lists, ignoring case, and a goal cannot be"
        + " related to itself. Without version a new relation is created; a pair of goals holds at"
        + " most one relation in each direction, so relating the same two goals again in the same"
        + " direction is refused (change that relation's type instead), and because Conflicts and"
        + " Duplicates hold in both directions, the reverse of an existing one of the same type is"
        + " refused too. With version the existing relation's type is updated, and the goal names"
        + " must then match exactly, including case. Nothing is returned: read the goal to see the"
        + " relation and its id.")
public record EditGoalRelationInput(
        @NotBlank String projectName,
        @NotBlank String fromGoalName,
        @NotBlank String toGoalName,
        @NotBlank @AllowedValues(GoalRelationType.class) String relationType,
        Integer version
) {
}
