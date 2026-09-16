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

import com.rreganjr.requel.service.api.CommandDescription;

/**
 * Input for EditTag. {@code tagId} null = create; {@code projectName} null/blank =
 * global/system tag. Category may be null (flat tag); value is required. Uses
 * {@code projectName} (not id) to match the rest of the project-scoped API. Both
 * {@code category} and {@code value} are normalized to a slug on write by
 * {@code TagNormalizer.slug} — see {@link CommandDescription} on this record for the
 * caller-facing statement of that.
 */
@CommandDescription("Creates or edits a single tag. Pass tagId to edit an existing tag, or leave it"
        + " null to create one; a null or blank projectName makes the tag global. category may be"
        + " null for a flat tag; value is required."
        + " Both category and value are stored as slugs: trimmed, lower-cased, and with each run of"
        + " non-alphanumeric characters collapsed to a single hyphen. \"CON-3685\" is stored as"
        + " \"con-3685\" and \"v2.0\" as \"v2-0\"."
        + " The slug is the uniqueness key and the text you supplied is not retained, so read the"
        + " response to see what was stored.")
public record EditTagInput(
        Long tagId,
        String projectName,
        String category,
        String value,
        String color
) {
}
