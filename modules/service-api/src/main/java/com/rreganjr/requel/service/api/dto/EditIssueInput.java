package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for EditIssue command. issueId null = create new issue on entity.
 */
public record EditIssueInput(
        String projectName,
        String entityType,
        Long entityId,
        Long issueId,
        String text,
        Boolean mustBeResolved
) {
}
