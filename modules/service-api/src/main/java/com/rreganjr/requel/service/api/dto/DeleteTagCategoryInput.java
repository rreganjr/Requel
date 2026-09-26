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
 * Input for DeleteTagCategory.
 */
@CommandDescription(value = "Deletes a tag category, selected by categoryId, and with it the"
        + " category's rules: exclusivity, allowed entity types and allowed values. Tags that use"
        + " the category name are neither deleted nor detached; they carry on as ordinary tags,"
        + " under a global category of the same name if there is one. An unknown categoryId changes"
        + " nothing.",
        authorization = "Annotation[Delete] for a project category; system administrator for a"
                + " global category")
public record DeleteTagCategoryInput(
        Long categoryId
) {
}
