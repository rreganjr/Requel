package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Read DTO for a position option on an issue.
 * positionType is the simple class name of the position (e.g. "PositionImpl",
 * "AddWordToDictionaryPosition", "ChangeSpellingPosition") so the UI can
 * label and dispatch the correct ResolveIssue command variant.
 */
public record PositionDto(
        Long id,
        int version,
        String text,
        String createdBy,
        String positionType,
        List<ArgumentDto> arguments
) {
}
