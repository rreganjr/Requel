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
 * Input for removing a "defers to" edge between two project sources (issue #273).
 *
 * @param projectName        the project
 * @param system             the system of the source that defers
 * @param externalId         its external id
 * @param defersToSystem     the system of the source it defers to
 * @param defersToExternalId its external id
 */
@CommandDescription(value = "Removes a defers-to edge recorded with AddSourceAuthority. Both sources"
        + " are kept.",
        authorization = "Project[Edit]")
public record RemoveSourceAuthorityInput(
        @NotBlank String projectName,
        @NotBlank @Size(max = 40) String system,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Size(max = 40) String defersToSystem,
        @NotBlank @Size(max = 255) String defersToExternalId
) {
}
