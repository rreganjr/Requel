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

import java.util.List;

/**
 * Issue #274: a project's whole normative content in one read, for gateway callers. Entities refer
 * to one another by id, so nothing is repeated; every list is sorted by id, so two reads of an
 * unchanged project are equal. Provenance and references are never included (#272 P7): read them
 * with {@code listSources} / {@code getEntitySources}.
 *
 * @param project       the project summary, with its entity counts
 * @param annotations   the annotation mode served: NONE, OPEN or ALL
 * @param characters    the characters this read counted against the cap
 * @param maxCharacters the cap ({@code requel.gateway.content.max-characters}); 0 when there is none
 * @param steps         the plain steps; a scenario used as a step is in {@code scenarios}
 */
public record ProjectContentDto(
        ProjectDto project,
        String annotations,
        int characters,
        int maxCharacters,
        List<StakeholderContentDto> stakeholders,
        List<GoalContentDto> goals,
        List<StoryContentDto> stories,
        List<ActorContentDto> actors,
        List<UseCaseContentDto> useCases,
        List<ScenarioContentDto> scenarios,
        List<StepContentDto> steps,
        List<GlossaryTermContentDto> glossary
) {
}
