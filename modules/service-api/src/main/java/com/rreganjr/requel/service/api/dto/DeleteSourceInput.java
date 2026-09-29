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
import jakarta.validation.constraints.Size;

/**
 * Input for deleting a project source (issue #273).
 *
 * @param projectName the project
 * @param system      the source's system
 * @param externalId  the source's external id
 */
@CommandDescription(value = "Deletes one of the project's sources or references, with its citations"
        + " and its defers-to edges. Refused while any entity was derived from the source"
        + " (DERIVED_FROM), so provenance is never lost by removing a reference; UnlinkSource those"
        + " links first if they are wrong.",
        authorization = "Project[Edit]")
public record DeleteSourceInput(
        @NotBlank String projectName,
        @NotBlank @Size(max = 40) String system,
        @NotBlank @Size(max = 255) String externalId
) {
}
