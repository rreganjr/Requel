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
 * One goal relation type, as served by {@code GET /api/projects/goal-relation-types} so a client
 * renders the vocabulary rather than keeping its own copy (issue #257).
 *
 * @param value        the name sent as {@code EditGoalRelation.relationType} and returned in
 *                     {@link GoalRelationDto#relationType()}, e.g. {@code "DependsOn"}
 * @param label        display name read from the from goal's side, e.g. "Depends on"
 * @param description  what the relation means, for someone choosing it
 * @param symmetric    true when the relation holds in both directions
 * @param inverseLabel display name read from the to goal's side, e.g. "Required by"; the same as
 *                     {@code label} for a symmetric type
 */
public record GoalRelationTypeDto(
        String value,
        String label,
        String description,
        boolean symmetric,
        String inverseLabel
) {
}
