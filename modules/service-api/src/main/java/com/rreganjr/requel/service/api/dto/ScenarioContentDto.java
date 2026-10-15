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

import java.util.List;

/**
 * Issue #274: a scenario in a project content read.
 *
 * @param steps the scenario's steps in order; a step shared by several scenarios has the same id
 *              in each, and a scenario used as a step is a {@code Scenario} reference
 */
public record ScenarioContentDto(
        Long id,
        int version,
        String name,
        String text,
        String scenarioType,
        List<StepRefDto> steps,
        List<String> tags,
        AnnotationsDto annotations
) {
}
