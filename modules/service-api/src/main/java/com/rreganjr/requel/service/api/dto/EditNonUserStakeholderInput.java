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
import jakarta.validation.constraints.NotBlank;
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for creating or editing a non-user stakeholder (external authority).
 *
 * <p>
 * On update, a null (or absent) {@code text} leaves it as it is; an empty string clears it
 * (issue #316).
 *
 * @param projectName    project to add the stakeholder to
 * @param stakeholderId  ID of the stakeholder to edit (null for create)
 * @param name           stakeholder name (the new name to set)
 * @param text           description of the stakeholder
 * @param version        optimistic lock version (null for create)
 */
@CommandDescription("Creates or edits a non-user stakeholder: a party with an interest in the"
        + " project who is not a Requel user, such as a regulator or an external authority. Leave"
        + " stakeholderId null to create one, or pass the id of an existing non-user stakeholder to"
        + " edit it; a user stakeholder's id is not accepted. name is required on every call, so"
        + " send the current name to keep it, and it is unique among the project's non-user"
        + " stakeholders, ignoring case."
        + CommandDescriptions.PARTIAL_UPDATE
        + CommandDescriptions.VERSION_CHECKED)
public record EditNonUserStakeholderInput(
        @NotBlank String projectName,
        Long stakeholderId,
        @NotBlank
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String text,
        Integer version
) {
}
