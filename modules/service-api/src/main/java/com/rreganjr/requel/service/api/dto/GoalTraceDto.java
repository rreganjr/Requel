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

import java.util.List;

/**
 * #497: one goal's row in the traceability matrix. Each list is sorted by name.
 *
 * @param realized whether a story or a use case references the goal; stakeholders and actors
 *                 referencing it don't realize it
 */
public record GoalTraceDto(
        Long id,
        String name,
        boolean realized,
        List<EntityReferenceDto> stakeholders,
        List<EntityReferenceDto> actors,
        List<EntityReferenceDto> stories,
        List<EntityReferenceDto> useCases
) {
}
