package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Read DTO for an issue annotation with its positions.
 */
public record IssueDto(
        Long id,
        int version,
        String text,
        boolean mustBeResolved,
        boolean resolved,
        String resolvedBy,
        String resolvedByPosition,
        String createdBy,
        List<PositionDto> positions
) {
}
