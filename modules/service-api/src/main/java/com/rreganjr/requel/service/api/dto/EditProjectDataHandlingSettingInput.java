/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

/**
 * Input for switching one of a project's data-handling settings on or off (issue #262).
 *
 * @param projectName the project
 * @param key {@code egress.external} or {@code redaction.<category>} (credentials, email, phone,
 *            ssn, card); see GET /api/projects/{name}/data-handling
 * @param enabled on: remote providers allowed / the category is masked
 */
@CommandDescription("Switches one of the project's AI data-handling settings on or off:"
        + " egress.external (whether project text may be sent to a remote AI provider) or"
        + " redaction.credentials|email|phone|ssn|card (whether that kind of sensitive text is"
        + " masked before any provider sees it). Every setting is on until switched off.")
public record EditProjectDataHandlingSettingInput(
        @NotBlank String projectName,
        @NotBlank String key,
        boolean enabled
) {
}
