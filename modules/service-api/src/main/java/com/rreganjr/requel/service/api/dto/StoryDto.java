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
 * Story DTO. The list view omits goals and actors for efficiency;
 * the detail view includes them.
 *
 * @param id          story id
 * @param version     optimistic lock version
 * @param name        story name
 * @param text        story body text
 * @param storyType   "Success" or "Exception"
 * @param createdBy   display name of creator
 * @param goals       associated goals (detail view only)
 * @param actors      associated actors (detail view only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryDto(
        Long id,
        int version,
        String name,
        String text,
        String storyType,
        String createdBy,
        List<EntityReferenceDto> goals,
        List<EntityReferenceDto> actors
) {
}
