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
 * Issue #348: one synonym of a glossary term and the term's referers whose name or text still
 * use it (whole words, ignoring case), so a client can offer {@code ReplaceTermText}.
 *
 * @param synonymId   the synonym (an alternate term) id
 * @param synonymName the synonym's name, the text to be replaced
 * @param referers    the referers that use it
 */
public record SynonymUseDto(
        Long synonymId,
        String synonymName,
        List<EntityReferenceDto> referers
) {
}
