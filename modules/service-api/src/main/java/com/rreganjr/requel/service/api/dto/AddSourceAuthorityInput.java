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
 * Input for recording that one project source defers to another (issue #273).
 *
 * @param projectName        the project
 * @param system             the system of the source that gives way
 * @param externalId         its external id
 * @param defersToSystem     the system of the source that wins where the two disagree
 * @param defersToExternalId its external id
 * @param note               a note on the edge, e.g. "operational detail"; null leaves an existing
 *                           note, "" clears it
 */
@CommandDescription(value = "Records that one of the project's sources defers to another: where the"
        + " two disagree, defersTo wins. Both remain current — this is precedence, not"
        + " replacement (e.g. a guide whose colophon says the runbook is correct if they ever"
        + " differ). Precedence is transitive, and compareSources resolves which of two sources"
        + " wins. Both sources must already be recorded with RecordSource. Repeating an edge"
        + " updates its note; a source deferring to itself, or an edge that would make a cycle,"
        + " is refused.",
        authorization = "Project[Edit]")
public record AddSourceAuthorityInput(
        @NotBlank String projectName,
        @NotBlank @Size(max = 40) String system,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Size(max = 40) String defersToSystem,
        @NotBlank @Size(max = 255) String defersToExternalId,
        @Size(max = 1000) String note
) {
}
