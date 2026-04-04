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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Read DTO for a glossary term.
 * List view omits alternateTerms and referers for efficiency; detail view includes them.
 *
 * @param id               term id
 * @param version          optimistic lock version
 * @param name             the term name (unique within a project)
 * @param text             the definition/description of the term
 * @param createdBy        display name of creator
 * @param canonicalTermId  id of the canonical (preferred) term this is an alternate for, or null
 * @param canonicalTermName name of the canonical term, or null
 * @param alternateTerms   terms that reference this term as their canonical (detail view only)
 * @param referers         project entities that use this term, identified by NLP analysis (detail view only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GlossaryTermDto(
        Long id,
        int version,
        String name,
        String text,
        String createdBy,
        Long canonicalTermId,
        String canonicalTermName,
        List<EntityReferenceDto> alternateTerms,
        List<EntityReferenceDto> referers
) {
}
