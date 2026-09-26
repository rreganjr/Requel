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

@CommandDescription("Removes an actor (actorId) from a container without deleting the actor."
        + " containerType is Project, UseCase or Story, ignoring case, and actorContainerId is that"
        + " entity's id. It removes the actor from the container's actors only and does not clear a"
        + " primary actor. Removing an actor that is not there changes nothing, and removing it"
        + " from the project container leaves it in the project. Returns the updated use case or"
        + " story, or nothing when the container is the project.")
public record RemoveActorFromActorContainerInput(
        @NotBlank String projectName,
        @NotNull Long actorContainerId,
        @NotNull Long actorId,
        @NotBlank String containerType
) {
}
