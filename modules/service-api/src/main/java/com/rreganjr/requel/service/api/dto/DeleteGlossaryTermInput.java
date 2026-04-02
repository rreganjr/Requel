package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for deleting a glossary term.
 */
public record DeleteGlossaryTermInput(
        String projectName,
        Long termId
) {
}
