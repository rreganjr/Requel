package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for creating or editing a glossary term.
 * Set termId null to create a new term.
 */
public record EditGlossaryTermInput(
        String projectName,
        Long termId,
        String name,
        String text,
        Long canonicalTermId
) {
}
