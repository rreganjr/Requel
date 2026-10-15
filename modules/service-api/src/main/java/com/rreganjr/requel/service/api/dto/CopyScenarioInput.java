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

@CommandDescription("Copies a scenario, selected by scenarioId, within its project. The copy is"
        + " named after the original with the first free number appended (\"Checkout 1\"), the name"
        + " cannot be chosen here, and the copy is not attached to any use case. Each step is"
        + " copied as a new step under a numbered name rather than shared, and a nested scenario is"
        + " copied as a single plain step, so its own steps are not carried over. Glossary terms"
        + " and annotations are shared with the original.")
public record CopyScenarioInput(@NotBlank String projectName, @NotNull Long scenarioId) {}
