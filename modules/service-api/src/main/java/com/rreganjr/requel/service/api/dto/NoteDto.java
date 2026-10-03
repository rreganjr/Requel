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
 * Read DTO for a note annotation.
 */
public record NoteDto(
        Long id,
        int version,
        String text,
        String createdBy,
        /** The provenance label: {@code ASSISTANT:<id>} for an assistant's, null for a person's (#270). */
        String source,
        /** True when an assistant raised it against text that has since changed, or no longer reports it: it may no longer apply (#270). */
        boolean stale,
        /** The assistant's display name when an assistant raised it, else null (#265). */
        String sourceName,
        /** REVIEW, POLICY or LEXICAL when an assistant raised it, else null (#265). */
        String sourceKind
) {
}
