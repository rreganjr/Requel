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
import java.util.Map;

/**
 * #504: one page of a project search, for the UI: the results from {@code offset}, at most
 * {@code limit} of them, the number of matches in all, and that number per entity type (always
 * complete, whatever the page).
 */
public record ProjectSearchPageDto(
        List<ProjectSearchResultDto> results,
        int total,
        Map<String, Integer> countsByType,
        int offset,
        int limit
) {
}
