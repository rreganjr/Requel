package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for EditPosition command. positionId null = create new position on issue.
 */
public record EditPositionInput(
        String projectName,
        Long issueId,
        Long positionId,
        String text
) {
}
