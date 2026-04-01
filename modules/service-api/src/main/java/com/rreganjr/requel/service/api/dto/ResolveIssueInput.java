package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for resolving an issue by choosing one of its positions.
 * The correct ResolveIssueCommand subtype is selected at runtime based on
 * the concrete position type loaded from the database.
 */
public record ResolveIssueInput(
        String projectName,
        Long issueId,
        Long positionId
) {
}
