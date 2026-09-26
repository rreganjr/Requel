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
import com.rreganjr.requel.service.api.CommandDescriptions;
import jakarta.validation.constraints.NotBlank;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input for EditActor command.
 * actorId is null for create, non-null for update.
 * version is null for create, required for update (optimistic lock check).
 * <p>
 * On update, a null (or absent) {@code description} leaves it as it is; an empty string
 * clears it (issue #316).
 */
@CommandDescription("Creates or edits an actor: someone or something that takes part in use cases"
        + " and stories. Leave actorId null to create one, or pass it to edit an existing actor."
        + " name is required on every call, so send the current name to keep it, and actor names"
        + " are unique within the project, ignoring case. description is the actor's text. Saving"
        + " asks the assistant to analyze the actor."
        + CommandDescriptions.PARTIAL_UPDATE
        + CommandDescriptions.VERSION_CHECKED)
public record EditActorInput(
        @NotBlank String projectName,
        Long actorId,
        @NotBlank
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String description,
        Integer version
) {
}
