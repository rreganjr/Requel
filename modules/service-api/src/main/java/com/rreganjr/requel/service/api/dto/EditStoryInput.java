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

import jakarta.validation.constraints.NotBlank;

/**
 * Input DTO for creating or editing a story.
 *
 * @param projectName      project the story belongs to
 * @param storyId          ID of the story to edit (null for create)
 * @param name             story name (the new name to set)
 * @param text             story body text
 * @param storyTypeName    "Success" or "Exception"
 * @param primaryActorName name of the primary actor (null to clear)
 * @param version          optimistic lock version (null for create)
 */
public record EditStoryInput(
        @NotBlank String projectName,
        Long storyId,
        @NotBlank String name,
        String text,
        String storyTypeName,
        // Deliberately unconstrained: a story's primary actor is optional, and passing null is how
        // the SPA CLEARS it (ProjectCommandRegistrar:364 applies it unconditionally, and
        // StoryPrimaryActorMappingTest asserts the clear). A @NotBlank here made every story edit
        // fail with 422 -- including stories that never had a primary actor. Note
        // EditUseCaseInput.primaryActorName is likewise unconstrained. Enforcing it was dormant and
        // therefore invisible until #171 turned DTO validation on.
        String primaryActorName,
        Integer version
) {
}
