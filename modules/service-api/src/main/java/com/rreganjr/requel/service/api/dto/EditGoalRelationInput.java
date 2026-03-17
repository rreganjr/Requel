package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for creating or editing a goal relation.
 *
 * @param projectName    project context
 * @param fromGoalName   name of the origin goal
 * @param toGoalName     name of the target goal
 * @param relationType   "Supports" or "Conflicts"
 * @param version        optimistic lock version (null for create)
 */
public record EditGoalRelationInput(
        String projectName,
        String fromGoalName,
        String toGoalName,
        String relationType,
        Integer version
) {
}
