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

import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.service.api.AllowedValues;
import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Input for removing an entity's link to a fragment of a source (issue #272).
 *
 * @param projectName the project
 * @param entityType  the entity's type, as for LinkSource
 * @param entityId    the entity's id
 * @param system      the source's system
 * @param externalId  the source's external id
 * @param fragment    the fragment the link names, or null for a whole-source link
 * @param relation    issue #273: the link's relation, DERIVED_FROM (the default) or CITES
 */
@CommandDescription(value = "Removes one entity's link to a fragment of a source — a derived-from link"
        + " recorded by mistake, or a citation (relation CITES) that no longer applies. The source"
        + " itself is kept; DeleteSource removes a reference.",
        authorization = "Edit on the entity's type (Scenario[Edit] for a step, Stakeholder[Edit]"
                + " for a stakeholder)")
public record UnlinkSourceInput(
        @NotBlank String projectName,
        @NotBlank
        @Pattern(regexp = "Goal|Story|Actor|UseCase|Scenario|Step|GlossaryTerm|NonUserStakeholder",
                message = "must be one of Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm,"
                        + " NonUserStakeholder")
        String entityType,
        @NotNull Long entityId,
        @NotBlank String system,
        @NotBlank String externalId,
        String fragment,
        @Pattern(regexp = "\\s*(DERIVED_FROM|CITES)\\s*", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of DERIVED_FROM, CITES")
        @AllowedValues(SourceLinkRelation.class)
        String relation
) {
}
