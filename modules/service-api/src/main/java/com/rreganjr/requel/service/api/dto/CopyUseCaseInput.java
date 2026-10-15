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

@CommandDescription("Copies a use case, selected by useCaseId, within its project. The copy is"
        + " named after the original with the first free number appended (\"Login 1\", \"Login"
        + " 2\"); the name cannot be chosen here, so rename the copy with EditUseCase. Its primary"
        + " scenario is copied, with its steps copied under numbered names too; its actors, goals,"
        + " stories, glossary terms and annotations are shared with the original rather than"
        + " copied. Additional scenarios are not carried over; attach them with"
        + " AddScenarioToUseCase.")
public record CopyUseCaseInput(@NotBlank String projectName, @NotNull Long useCaseId) {}
