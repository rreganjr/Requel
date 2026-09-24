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

/**
 * Input for adding a word to a project's dictionary (issue #319).
 *
 * @param projectName the project whose dictionary receives the word
 * @param lemma the word: one token, at most 80 characters
 */
@CommandDescription("Adds a word to a project's dictionary so the spell checker accepts it in that"
        + " project only. One word, no spaces, at most 80 characters. Adding a word the project"
        + " already has, in any case, returns the existing entry. Requires Project Edit.")
public record AddProjectDictionaryWordInput(
        @NotBlank String projectName,
        @NotBlank String lemma
) {
}
