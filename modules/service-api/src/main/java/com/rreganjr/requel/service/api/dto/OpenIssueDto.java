package com.rreganjr.requel.service.api.dto;

/**
 * A single unresolved issue on a project entity, for the project-wide open issues view.
 *
 * @param issueId        annotation id of the issue
 * @param issueText      the issue description text
 * @param mustBeResolved whether this issue must be resolved before the project is complete
 * @param entityType     simple interface name of the annotated entity (e.g. "Goal", "Story")
 * @param entityId       id of the annotated entity
 * @param entityName     name of the annotated entity
 */
public record OpenIssueDto(
        Long issueId,
        String issueText,
        boolean mustBeResolved,
        String entityType,
        Long entityId,
        String entityName
) {
}
