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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Issue #483: a plain step in the project's step list, which the scenario editor suggests when a
 * new step is named, so an existing step can be reused. {@code usedIn} is the number of scenarios
 * that use the step.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepSummaryDto(
    Long id,
    String name,
    String text,
    String scenarioType,
    int usedIn
) {}
