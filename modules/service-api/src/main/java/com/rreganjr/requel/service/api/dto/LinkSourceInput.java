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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for linking an existing entity to a fragment of a recorded source (issue #272).
 *
 * @param projectName  the project
 * @param entityType   Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm or
 *                     NonUserStakeholder
 * @param entityId     the entity's id
 * @param system       the source's system
 * @param externalId   the source's external id
 * @param fragment     a stable key for the part of the source, e.g. AC-4 or p.12; null for the
 *                     whole source
 * @param fragmentText the fragment's text as the entity reflects it, or null
 */
@CommandDescription(value = "Links an existing entity to a fragment of a source recorded with"
        + " RecordSource, as the entity is now — for adopting an entity built before provenance"
        + " was recorded. It never edits the entity or raises a conflict; UpsertFromSource is the"
        + " ingest path. Pass fragmentText so a later UpsertFromSource can tell whether the"
        + " fragment changed.",
        authorization = "Edit on the entity's type (Scenario[Edit] for a step, Stakeholder[Edit]"
                + " for a stakeholder)")
public record LinkSourceInput(
        @NotBlank String projectName,
        @NotBlank
        @Pattern(regexp = "Goal|Story|Actor|UseCase|Scenario|Step|GlossaryTerm|NonUserStakeholder",
                message = "must be one of Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm,"
                        + " NonUserStakeholder")
        String entityType,
        @NotNull Long entityId,
        @NotBlank String system,
        @NotBlank String externalId,
        @Size(max = 255) String fragment,
        String fragmentText
) {
}
