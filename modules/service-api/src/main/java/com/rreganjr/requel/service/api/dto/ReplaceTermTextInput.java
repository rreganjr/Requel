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

import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input for {@code ReplaceTermText} (issue #348).
 *
 * @param projectName the project
 * @param termId      the glossary term whose name is the replacement
 * @param synonymId   one of the term's synonyms (alternate terms), the text to replace
 * @param referers    the referers to change (from the term's {@code synonymUses}), by
 *                    {@code entityType} and {@code id}
 */
@CommandDescription(value = "Replaces a synonym of a glossary term with the term's name in the term's"
        + " referers, matching whole words and ignoring case. Renaming a term keeps its old name"
        + " as a synonym; the term's synonymUses lists the referers still using it. Each referer"
        + " is changed through its own edit, so one the caller may not edit is refused and the"
        + " rest are changed. The result lists applied, refused and unchanged referers.",
        authorization = "Edit on each referer's type (e.g. Goal[Edit], Story[Edit]); referers"
                + " the caller may not edit are refused")
public record ReplaceTermTextInput(
        @NotBlank String projectName,
        @NotNull Long termId,
        @NotNull Long synonymId,
        List<EntityReferenceDto> referers
) {
}
