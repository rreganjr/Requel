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
 * Input DTO for EditReportGenerator command.
 * reportId is null when creating a new report generator.
 * <p>
 * On update, a null (or absent) {@code text} leaves it as it is; an empty string clears it
 * (issue #316).
 */
@CommandDescription("Creates or edits a report generator: a named XSLT stylesheet that turns the"
        + " project's XML export into a report. Leave reportId null to create one, or pass it to"
        + " edit an existing one. name is required on every call, so send the current name to keep"
        + " it, and it is unique within the project, ignoring case. text is the stylesheet, stored"
        + " as given and not checked. This stores the definition only; reports are not generated"
        + " through the gateway."
        + CommandDescriptions.PARTIAL_UPDATE)
public record EditReportGeneratorInput(
        @NotBlank String projectName,
        Long reportId,
        @NotBlank
        @Size(max = ValidationLimits.ARTIFACT_NAME_MAX, message = ValidationLimits.LENGTH_MESSAGE)
        String name,
        String text
) {
}
