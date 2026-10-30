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

/**
 * Input DTO for resolving an issue by choosing one of its positions.
 * The correct ResolveIssueCommand subtype is selected at runtime based on
 * the concrete position type loaded from the database.
 */
@CommandDescription(value = "Resolves the issue issueId by accepting one of its positions, positionId (an"
        + " issue's positions come from getAnnotations or getOpenIssues, each with a positionType)."
        + " Accepting a position does what it proposes: Position only marks the issue resolved;"
        + " IgnorePosition also keeps the assistant from raising that finding on the entity again;"
        + " ChangeSpellingPosition rewrites the word in every entity the issue is on"
        + " (all or nothing: refused if you can't edit one of them); AddWordToDictionaryPosition"
        + " adds the word to the project's dictionary; AddActorPosition adds the phrase as an actor"
        + " in the entities the issue is on; AddGlossaryTermPosition adds it to the glossary."
        + " Needs Annotation[Edit] on the project, plus Edit on what the position changes. A"
        + " position that isn't one of the issue's, or an issue in another project, is refused."
        + " Resolving an issue that is already resolved records the new position.",
        authorization = "Annotation[Edit], plus Edit on what the accepted position changes:"
                + " ChangeSpellingPosition, every entity the issue is on; AddActorPosition,"
                + " Actor[Edit]; AddGlossaryTermPosition, GlossaryTerm[Edit]")
public record ResolveIssueInput(
        @NotBlank String projectName,
        @NotNull Long issueId,
        @NotNull Long positionId
) {
}
