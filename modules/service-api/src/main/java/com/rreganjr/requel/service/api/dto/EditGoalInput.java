package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for creating or editing a goal.
 *
 * @param projectName  project the goal belongs to
 * @param name         goal name (used for both display and lookup on edit)
 * @param text         goal description/body
 * @param version      optimistic lock version (null for create)
 */
public record EditGoalInput(
        String projectName,
        String name,
        String text,
        Integer version
) {
}
