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

@CommandDescription("Adds an existing actor (actorId) to a container. containerType is Project,"
        + " UseCase or Story, ignoring case, and actorContainerId is the project's id or the id of"
        + " a use case or story in the project; any other type is refused. On a use case or story"
        + " this adds the actor to its actors, never as the primary actor, which EditUseCase and"
        + " EditStory set. Adding an actor that is already there changes nothing. Returns the"
        + " updated use case or story, or nothing when the container is the project.")
public record AddActorToActorContainerInput(
        @NotBlank String projectName,
        @NotNull Long actorContainerId,
        @NotNull Long actorId,
        @NotBlank String containerType
) {
}
