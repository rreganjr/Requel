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
 * Result of {@code ReplaceTermText} (issue #348).
 *
 * @param applied   referers changed
 * @param refused   referers the caller may not edit; nothing was written to them
 * @param unchanged referers that aren't the term's, or no longer use the synonym
 */
public record ReplaceTermTextResultDto(
        List<EntityReferenceDto> applied,
        List<EntityReferenceDto> refused,
        List<EntityReferenceDto> unchanged
) {
}
