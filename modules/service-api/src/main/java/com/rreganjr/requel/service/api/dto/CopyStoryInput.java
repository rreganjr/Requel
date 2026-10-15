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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@CommandDescription("Copies a story, selected by storyId, within its project. The copy gets the"
        + " original's text, type, actors, goals and glossary terms and shares its annotations, but"
        + " not its primary actor or its use cases. newStoryName is optional and defaults to the"
        + " original's name. A name already taken gets a number appended (\"Login 1\", \"Login 2\")"
        + " instead of being refused, so read the response for the name used.")
public record CopyStoryInput(
        @NotBlank String projectName,
        @NotNull Long storyId,
        String newStoryName
) {
}
