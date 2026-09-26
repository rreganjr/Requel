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

@CommandDescription("Attaches an existing scenario to a use case as an additional scenario, such"
        + " as an alternative or exception flow, by useCaseId and scenarioId. The scenario is"
        + " linked, not copied, so a change to it shows in every use case that uses it. Attaching"
        + " one that is already attached changes nothing. The primary scenario is not checked for,"
        + " so do not attach it here. Create the scenario first with EditScenario.")
public record AddScenarioToUseCaseInput(
        @NotBlank String projectName,
        @NotNull Long useCaseId,
        @NotNull Long scenarioId
) {}
