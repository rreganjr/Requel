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
import com.rreganjr.validator.ValidationLimits;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for the EditProject command. Used for both creating and updating projects.
 * {@code projectName} selects the project to update; when it is null or names no project, a new
 * project is created. {@code id} is not read (issue #296). A non-null {@code version} is checked
 * on update.
 * Organization is referenced by organizationId when selecting an existing org,
 * or by organizationName when creating a new one.
 * <p>
 * On update, a null (or absent) {@code name} or {@code description} leaves it as it is and
 * an empty {@code description} clears it; with neither {@code organizationId} nor
 * {@code organizationName} the organization is left as it is, and an empty
 * {@code organizationName} clears it (issue #316).
 */
@CommandDescription(value = "Creates or edits a project. projectName selects the project to edit;"
        + " when it is null or names no project, a new project is created, so a misspelled"
        + " projectName creates a project rather than failing. name is the new name: required when"
        + " creating, kept when left null, and it cannot be cleared. Project names are unique"
        + " across the whole installation, ignoring case. The id field is ignored. organizationId"
        + " selects an existing organization; otherwise organizationName is looked up and created"
        + " when it does not exist, and an empty organizationName removes the organization."
        + " Creating a project makes you a stakeholder with every permission and adds the assistant"
        + " stakeholder and a built-in HTML Specification report."
        + CommandDescriptions.PARTIAL_UPDATE
        + CommandDescriptions.VERSION_CHECKED,
        authorization = "createProjects role permission to create; Project[Edit] to edit")
public record EditProjectInput(
        Long id,
        Integer version,
        String projectName,
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String description,
        Long organizationId,
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String organizationName
) {
}
