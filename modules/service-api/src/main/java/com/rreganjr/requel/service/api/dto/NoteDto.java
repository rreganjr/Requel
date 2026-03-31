package com.rreganjr.requel.service.api.dto;

/**
 * Read DTO for a note annotation.
 */
public record NoteDto(
        Long id,
        int version,
        String text,
        String createdBy
) {
}
