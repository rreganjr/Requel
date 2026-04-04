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
 * A single unresolved issue on a project entity, for the project-wide open issues view.
 *
 * @param issueId        annotation id of the issue
 * @param issueText      the issue description text
 * @param mustBeResolved whether this issue must be resolved before the project is complete
 * @param entityType     simple interface name of the annotated entity (e.g. "Goal", "Story")
 * @param entityId       id of the annotated entity
 * @param entityName     name of the annotated entity
 */
public record OpenIssueDto(
        Long issueId,
        String issueText,
        boolean mustBeResolved,
        String entityType,
        Long entityId,
        String entityName
) {
}
