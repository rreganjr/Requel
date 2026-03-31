package com.rreganjr.requel.service.api.dto;

/**
 * Read DTO for an argument on a position.
 */
public record ArgumentDto(
        Long id,
        int version,
        String text,
        String supportLevel,
        String createdBy
) {
}
