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
import com.rreganjr.requel.service.api.CommandDescriptions;
import com.rreganjr.requel.service.api.FromEntityProperty;
import jakarta.validation.constraints.NotBlank;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for creating or editing a glossary term.
 * Set termId null to create a new term.
 * <p>
 * On update, a null (or absent) {@code text} leaves the definition as it is; an empty
 * string clears it (issue #316).
 */
@CommandDescription("Creates or edits a glossary term. Leave termId null to create one, or pass"
        + " it to edit an existing term. name is required on every call, so send the current name"
        + " to keep it, and term names are unique within the project, ignoring case."
        + " canonicalTermId makes this term an alternate of another term in the project; leaving it"
        + " null keeps any existing link, and this command cannot remove one."
        + CommandDescriptions.PARTIAL_UPDATE)
public record EditGlossaryTermInput(
        @NotBlank String projectName,
        Long termId,
        @NotBlank
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String text,
        @FromEntityProperty("canonicalTerm")
        Long canonicalTermId
) {
}
