package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for EditNote command. noteId null = create new note on entity.
 */
public record EditNoteInput(
        String projectName,
        String entityType,
        Long entityId,
        Long noteId,
        String text
) {
}
