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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One of a project's assistant definitions as the authoring page shows it (issue #264).
 *
 * @param source BUNDLED or PROJECT
 * @param version the content version, recorded on each run
 * @param forkedFromVersion the bundled version a copy was made from, or null
 * @param bundledVersion the bundled key's current version, or null for a project-only key;
 *        above forkedFromVersion means the bundled definition is newer
 * @param lockVersion send back on edit, revert and delete (0 for a bundled definition)
 * @param inEffectFor the entity types (set kinds for CORPUS) it covers in the project
 * @param enabled the project's switch (#268), or null where not read
 */
public record ProjectDefinitionDto(
        String key,
        String displayName,
        String kind,
        String taskType,
        Set<String> scope,
        List<String> contextProviders,
        Map<String, Integer> contextBudgets,
        String instructions,
        List<AssistantDefinitionVocabularyInput> vocabulary,
        boolean localOnly,
        String source,
        int version,
        Integer forkedFromVersion,
        Integer bundledVersion,
        int lockVersion,
        List<String> inEffectFor,
        Boolean enabled
) {
}
