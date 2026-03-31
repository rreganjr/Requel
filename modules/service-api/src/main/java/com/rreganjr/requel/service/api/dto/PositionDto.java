package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Read DTO for a position option on an issue.
 */
public record PositionDto(
        Long id,
        int version,
        String text,
        String createdBy,
        List<ArgumentDto> arguments
) {
}
