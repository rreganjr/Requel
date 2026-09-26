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
import jakarta.validation.constraints.NotNull;

@CommandDescription("Deletes a scenario, selected by scenarioId, removing it from every scenario"
        + " it is nested in and from every use case that has it as an additional scenario. Deleting"
        + " a use case's primary scenario fails; give the use case another primary scenario with"
        + " SetPrimaryScenarioOnUseCase first. Scenarios nested inside it are kept on their own."
        + CommandDescriptions.VERSION_CHECKED_ON_DELETE)
public record DeleteScenarioInput(@NotBlank String projectName, @NotNull Long scenarioId, Integer version) {}
