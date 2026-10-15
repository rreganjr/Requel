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
 * Input DTO for deleting a glossary term.
 */
@CommandDescription("Deletes a glossary term, selected by termId. Notes and issues on it are"
        + " removed, and deleted when nothing else carries them; entities that referred to it stop"
        + " doing so, and terms that had it as their canonical term stand alone. The assistant's"
        + " pending suggestion to add a term of that name to the glossary is removed too.")
public record DeleteGlossaryTermInput(
        @NotBlank String projectName,
        @NotNull Long termId
) {
}
