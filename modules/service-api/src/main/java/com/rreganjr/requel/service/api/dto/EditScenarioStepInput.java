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
import com.rreganjr.validator.ValidationLimits;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input for EditScenarioStep (issue #476): edits one existing plain step, wherever it is used.
 * Through {@code EditScenario} a caller has to resend a scenario's whole step list to change one
 * step, and a step it leaves out is deleted; this edits the one step alone.
 */
@CommandDescription("Edits one existing plain scenario step: its name, notes (text) or type."
        + " The step can be shared between scenarios, and the change shows in every scenario that"
        + " uses it. stepId must be a plain step, not a scenario; edit a scenario, or add, remove"
        + " and reorder its steps, with EditScenario. Pass version to refuse the edit if the step"
        + " changed since you read it. Step and scenario names are unique together within a"
        + " project, so a name already in use is refused."
        + CommandDescriptions.PARTIAL_UPDATE)
public record EditScenarioStepInput(
    @NotBlank String projectName,
    @NotNull Long stepId,
    Integer version,
    @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
    String name,
    String text,
    String scenarioTypeName
) {}
