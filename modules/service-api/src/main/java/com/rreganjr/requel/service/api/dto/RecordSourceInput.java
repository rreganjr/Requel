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

import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.service.api.AllowedValues;
import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for recording a project's external source (issue #272).
 *
 * @param projectName the project
 * @param system      the source family, e.g. jira, github, doc; stored lower-case
 * @param externalId  the source's own identifier, e.g. CON-3685; stored and matched exactly
 * @param locatorType URL or PATH; required with a locator
 * @param locator     an http(s) URL, or a relative file path; never read by Requel
 * @param title       a human title, or null to leave it as it is
 * @param contentHash the source's current content hash (for a file, the SHA-256 of its bytes),
 *                    or null; the result's {@code changed} compares it with the last one
 */
@CommandDescription("Records an external source the project's entities are built from — a ticket,"
        + " a guide, a review — identified by system and externalId (exact, case-sensitive). Pass"
        + " contentHash each time you read the source: the result's changed flag says whether it"
        + " differs from the last version recorded, without reading any entity. Null locator,"
        + " title or contentHash leave the recorded value as it is. The locator is stored for"
        + " people and is never passed to a model.")
public record RecordSourceInput(
        @NotBlank String projectName,
        @NotBlank @Size(max = 40) String system,
        @NotBlank @Size(max = 255) String externalId,
        @Pattern(regexp = "\\s*(URL|PATH)\\s*", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of URL, PATH")
        @AllowedValues(SourceLocatorType.class)
        String locatorType,
        @Size(max = 2048) String locator,
        @Size(max = 255) String title,
        @Size(max = 128) String contentHash
) {
}
