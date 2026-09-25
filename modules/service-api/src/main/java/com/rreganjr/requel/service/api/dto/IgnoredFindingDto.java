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

import java.util.Date;

/**
 * One ignored assistant finding (issue #320), for the project dictionary page.
 *
 * @param id the ignore's id, for DeleteIgnoredFinding
 * @param subject what was ignored: a word, a phrase or a sentence snippet
 * @param findingType e.g. unknown-word, vague-word, complex-text, glossary-term
 * @param entityType the entity type, e.g. Goal
 * @param entityId the entity's id
 * @param entityName the entity's name, or null if it can't be read
 * @param propertyName the property (Name, Text), or null for glossary terms
 * @param createdBy the username of whoever ignored it, or null
 * @param dateCreated when it was ignored
 */
public record IgnoredFindingDto(
        Long id,
        String subject,
        String findingType,
        String entityType,
        Long entityId,
        String entityName,
        String propertyName,
        String createdBy,
        Date dateCreated
) {
}
