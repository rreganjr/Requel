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

/**
 * Input for ConvertStepToScenario (issue #252). Promotes an existing plain step into a sub-scenario
 * in place, updating the scenarios that reference it.
 *
 * <p>This is the one step operation the {@code EditScenario} steps array cannot express. Sending
 * {@code isScenario: true} with an existing {@code stepId} there routes to a scenario lookup that
 * walks the project's scenarios and throws for a plain step's id, so without this command there is
 * no path from a plain step to a sub-scenario through the gateway.
 */
@CommandDescription("Promotes an existing plain scenario step into a sub-scenario in place, keeping"
        + " its name and text and updating the scenarios that reference it."
        + " stepId must be an existing plain step, not a scenario; converting a step that is already"
        + " a sub-scenario is not what this does."
        + " To create, edit or delete ordinary steps, use EditScenario and send the whole steps"
        + " array: the list is replaced on save, so omitting a step deletes it.")
public record ConvertStepToScenarioInput(
        String projectName,
        Long stepId
) {
}
