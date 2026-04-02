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
