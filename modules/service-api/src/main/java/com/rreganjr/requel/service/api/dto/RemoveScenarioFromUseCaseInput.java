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

@CommandDescription("Detaches an additional scenario from a use case, by useCaseId and"
        + " scenarioId, and keeps the scenario in the project; DeleteScenario removes it. Only"
        + " additional scenarios are affected: the use case's primary scenario, or a scenario that"
        + " is not attached, changes nothing and reports no error. SetPrimaryScenarioOnUseCase"
        + " replaces the primary scenario.")
public record RemoveScenarioFromUseCaseInput(
        @NotBlank String projectName,
        @NotNull Long useCaseId,
        @NotNull Long scenarioId
) {}
