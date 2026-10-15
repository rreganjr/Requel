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

/**
 * Input DTO for EditArgument command. argumentId null = create new argument on position.
 */
@CommandDescription("Creates or edits an argument for or against the position given by"
        + " positionId. supportLevel is exactly one of StronglyFor, For, Neutral, Against or"
        + " StronglyAgainst. Leave argumentId null to create one, or pass it to replace an"
        + " argument's text and supportLevel; an argument stays on the position it was created on."
        + " An unknown argumentId creates a new argument rather than failing.")
public record EditArgumentInput(
        String projectName,
        @NotNull Long positionId,
        Long argumentId,
        @NotBlank String text,
        @NotBlank String supportLevel
) {
}
