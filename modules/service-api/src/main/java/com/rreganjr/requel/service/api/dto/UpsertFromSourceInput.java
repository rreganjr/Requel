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

import java.util.Map;

import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.service.api.AllowedValues;
import com.rreganjr.requel.service.api.CommandDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for creating or updating an entity from a fragment of an external source (issue #272).
 *
 * @param projectName   the project
 * @param command       the edit command that writes the entity: EditGoal, EditStory, EditActor,
 *                      EditUseCase, EditScenario, EditGlossaryTerm or EditNonUserStakeholder
 * @param input         that command's input, without its id (Requel fills it in)
 * @param system        the source family, e.g. jira
 * @param externalId    the source's own identifier, e.g. CON-3685
 * @param locatorType   URL or PATH, with a locator
 * @param locator       an http(s) URL or a relative path; never read by Requel. Only used when
 *                      this call first records the source
 * @param title         a human title for the source; likewise only on first record
 * @param sourceVersion the source's current content hash, or null
 * @param fragment      a stable key for the part of the source, e.g. AC-4; null for the whole
 *                      source
 * @param fragmentText  the fragment's current text: what a change is detected against
 * @param entityId      which entity to update when several came from the fragment; else null
 */
@CommandDescription(value = "Creates or updates an entity from a fragment of an external source, with"
        + " Requel as the authority. Returns status: CREATED (no entity came from this fragment"
        + " yet), UNCHANGED (the fragment's text is what was ingested; nothing edited), UPDATED (it"
        + " changed and the entity was not edited in Requel since), CONFLICT (it changed and the"
        + " entity was edited in Requel since: the entity is left alone and an issue carrying the"
        + " new wording is raised on it) or AMBIGUOUS (several entities came from the fragment:"
        + " pass entityId, one of candidates). input is the edit command's own input without its"
        + " id. fragment must be a stable key for the part of the source — not a position that"
        + " renumbers when an item is inserted — or a re-ingest creates a new entity. Pass"
        + " sourceVersion (a content hash) to record the source's version. locator and title are"
        + " set when the source is first recorded; change them later with RecordSource. The locator"
        + " is stored for people and is never passed to a model.",
        authorization = "Edit on the entity's type (Stakeholder[Edit] for a stakeholder), plus"
                + " whatever the edit command requires")
public record UpsertFromSourceInput(
        @NotBlank String projectName,
        @NotBlank
        @Pattern(regexp = "EditGoal|EditStory|EditActor|EditUseCase|EditScenario|EditGlossaryTerm"
                + "|EditNonUserStakeholder",
                message = "must be one of EditGoal, EditStory, EditActor, EditUseCase, EditScenario,"
                        + " EditGlossaryTerm, EditNonUserStakeholder")
        String command,
        @NotNull Map<String, Object> input,
        @NotBlank @Size(max = 40) String system,
        @NotBlank @Size(max = 255) String externalId,
        @Pattern(regexp = "\\s*(URL|PATH)\\s*", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of URL, PATH")
        @AllowedValues(SourceLocatorType.class)
        String locatorType,
        @Size(max = 2048) String locator,
        @Size(max = 255) String title,
        @Size(max = 128) String sourceVersion,
        @Size(max = 255) String fragment,
        @NotBlank String fragmentText,
        Long entityId
) {
}
