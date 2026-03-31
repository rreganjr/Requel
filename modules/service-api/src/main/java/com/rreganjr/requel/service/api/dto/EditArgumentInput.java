package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for EditArgument command. argumentId null = create new argument on position.
 */
public record EditArgumentInput(
        String projectName,
        Long positionId,
        Long argumentId,
        String text,
        String supportLevel
) {
}
