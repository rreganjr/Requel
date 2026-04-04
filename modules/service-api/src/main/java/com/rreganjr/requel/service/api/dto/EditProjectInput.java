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
 * Input DTO for the EditProject command. Used for both creating and updating projects.
 * When id is provided, the matching project is updated; otherwise a new project is created.
 * The version field is required for updates to support optimistic locking.
 * Organization is referenced by organizationId when selecting an existing org,
 * or by organizationName when creating a new one.
 */
public record EditProjectInput(
        Long id,
        Integer version,
        String projectName,
        String name,
        String description,
        Long organizationId,
        String organizationName
) {
}
