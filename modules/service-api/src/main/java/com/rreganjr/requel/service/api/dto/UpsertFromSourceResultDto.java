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
 * The result of UpsertFromSource (issue #272).
 *
 * @param status        CREATED, UNCHANGED, UPDATED, CONFLICT or AMBIGUOUS
 * @param entityType    the entity's type, e.g. Goal
 * @param entityId      the entity's id; null when AMBIGUOUS
 * @param entityName    the entity's name; null when AMBIGUOUS
 * @param entity        the edit command's own result (e.g. a GoalDto) when CREATED or UPDATED
 * @param sourceChanged the supplied sourceVersion differs from the last one recorded
 * @param issueId       the conflict issue, when CONFLICT
 * @param candidates    the entity ids that came from the fragment, when AMBIGUOUS
 * @param source        the source as recorded
 */
public record UpsertFromSourceResultDto(
        String status,
        String entityType,
        Long entityId,
        String entityName,
        Object entity,
        boolean sourceChanged,
        Long issueId,
        List<Long> candidates,
        ExternalSourceDto source
) {
}
