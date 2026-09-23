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

import com.rreganjr.requel.service.api.FromEntityProperty;
import jakarta.validation.constraints.NotBlank;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input for the EditUseCase command. {@code useCaseId} null = create.
 * <p>
 * On update, a null (or absent) {@code text} leaves it as it is; an empty string clears it
 * (issue #316). A null {@code primaryActorName} leaves the actor as it is; an empty one is
 * refused, because a use case must have a primary actor; an unknown name creates the actor
 * (issue #325). The input has no steps, so a use-case save never touches its primary
 * scenario's steps; renaming the use case renames the scenario too only if it carried the old
 * name.
 */
public record EditUseCaseInput(
        @NotBlank String projectName,
        Long useCaseId,
        @NotBlank
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String text,
        @FromEntityProperty("primaryActor")
        String primaryActorName,
        Integer version
) {}
