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
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input for EditTagCategory. {@code categoryId} null = create; {@code projectName} null/blank =
 * global. The {@code name} and every entry in {@code values} are normalized to a slug on write by
 * {@code TagNormalizer.slug} — see {@link CommandDescription} on this record for the caller-facing
 * statement of that, which is what an MCP client and the CLI actually see.
 * {@code allowedEntityTypes}/{@code values} may be null or empty (no restriction).
 */
@CommandDescription(value = "Creates or edits a tag category and its controlled values. Pass"
        + " categoryId to edit an existing category, or leave it null to create one; a null or"
        + " blank projectName makes the category global. The name and every entry in values are"
        + " stored as slugs: trimmed, lower-cased, and with each run of non-alphanumeric characters"
        + " collapsed to a single hyphen. \"Source\" is stored as \"source\", \"CON-3685\" as"
        + " \"con-3685\", \"v2.0\" as \"v2-0\". The slug is the uniqueness key and the text you"
        + " supplied is not retained, so read the response to see what was stored.",
        authorization = "Annotation[Edit] for a project category; system administrator for a"
                + " global category")
public record EditTagCategoryInput(
        Long categoryId,
        String projectName,
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        boolean exclusive,
        String color,
        List<String> allowedEntityTypes,
        List<String> values
) {
}
