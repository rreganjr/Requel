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

/**
 * #504: one project search match. Its first three fields are {@link EntityReferenceDto}'s, so a
 * reader of the pre-#504 name-only search still finds what it read.
 *
 * @param entityType    Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm, Stakeholder or
 *                      Issue
 * @param id            the entity's (or issue's) id
 * @param name          the name as it reads (a step's with its actor role filled in); for an issue
 *                      the start of its text
 * @param matchedField  {@code name} or {@code text}
 * @param snippet       a short piece of the matched field around the first match, with an ellipsis
 *                      where it was cut
 * @param matchStart    where the match starts in {@code snippet}, or -1 if it can't be placed
 * @param matchLength   the match's length in {@code snippet}
 * @param resolved      for an issue, whether it is resolved; null otherwise
 * @param onEntityType  for an issue, the type of the entity it is on
 * @param onEntityId    for an issue, that entity's id
 * @param onEntityName  for an issue, that entity's name
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectSearchResultDto(
        String entityType,
        Long id,
        String name,
        String matchedField,
        String snippet,
        int matchStart,
        int matchLength,
        Boolean resolved,
        String onEntityType,
        Long onEntityId,
        String onEntityName
) {
}
