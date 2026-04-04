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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Unified DTO for both user and non-user stakeholders.
 * Uses nested type-specific objects rather than flat nullable fields —
 * see UI_DESIGN_GUIDE.md §14 "Polymorphic DTOs" for rationale.
 *
 * @param id                  stakeholder id
 * @param version             optimistic lock version
 * @param name                display name
 * @param type                "user" or "non-user"
 * @param createdBy           display name of the user who created this stakeholder
 * @param userDetails         non-null for user stakeholders; null for non-user
 * @param nonUserDetails      non-null for non-user stakeholders; null for user
 * @param goals               goals this stakeholder is concerned with (null on list endpoints)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StakeholderDto(
        Long id,
        int version,
        String name,
        String type,
        String createdBy,
        UserStakeholderDetails userDetails,
        NonUserStakeholderDetails nonUserDetails,
        List<EntityReferenceDto> goals
) {
}
