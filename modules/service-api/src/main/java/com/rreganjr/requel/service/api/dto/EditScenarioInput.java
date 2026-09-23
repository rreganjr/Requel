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

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import com.rreganjr.requel.service.api.CommandDescription;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input for EditScenario. {@code scenarioId} null = create. The {@code steps} array is the whole
 * step list: it replaces what the scenario had, so a step left out of it is removed.
 *
 * <p>Names are unique across both steps and scenarios within a project — they share one table and
 * one {@code (projectordomain_id, name)} constraint — and a name already in use is refused rather
 * than reused (issue #254). Link the existing entity by id instead: a step by its {@code stepId},
 * a nested scenario by its id as {@code stepId} with {@code isScenario} true.
 * <p>
 * On update, a null (or absent) {@code text} or {@code scenarioTypeName} leaves it as it
 * is; an empty {@code text} clears it (issue #316).
 */
@CommandDescription("Creates or edits a scenario and its list of steps. Pass scenarioId to edit an"
        + " existing scenario, or leave it null to create one."
        + " The steps array is the entire step list and replaces what the scenario had: an entry"
        + " with no stepId creates a step, an entry with a stepId edits or links that one, and a"
        + " step you leave out is removed from this scenario."
        + " Step and scenario names are unique together within a project. A new entry whose name is"
        + " already taken is refused, not reused, because steps are shared between scenarios and"
        + " editing one through the wrong scenario would change it everywhere. Link the existing"
        + " one instead: send its stepId for a step, or its id as stepId with isScenario true to"
        + " nest an existing scenario. The refusal names the id to use.")
public record EditScenarioInput(
    @NotBlank String projectName,
    Long scenarioId,
    @NotBlank
    @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
    String name,
    String text,
    String scenarioTypeName,
    Integer version,
    List<EditStepInput> steps
) {}
