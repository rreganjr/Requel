package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for the EditProject command. Used for both creating and updating projects.
 * When projectName matches an existing project, it is updated; otherwise a new project is created.
 */
public record EditProjectInput(
        String projectName,
        String name,
        String description,
        String organizationName
) {
}
