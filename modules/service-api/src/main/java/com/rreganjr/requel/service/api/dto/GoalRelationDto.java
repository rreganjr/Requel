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

/**
 * A relationship between two goals.
 * Used in the goal detail view — relationsFromThisGoal shows outgoing,
 * relationsToThisGoal shows incoming.
 *
 * @param id            relation id
 * @param version       optimistic lock version
 * @param goalId        the other goal's id (toGoal for outgoing, fromGoal for incoming)
 * @param goalName      the other goal's display name
 * @param relationType  a {@code GoalRelationType} name, e.g. "Supports"; the labels, and whether
 *                      the type is symmetric, come from {@code GET /api/projects/goal-relation-types}
 */
public record GoalRelationDto(
        Long id,
        int version,
        Long goalId,
        String goalName,
        String relationType
) {
}
