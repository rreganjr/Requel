package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for the EditProject command. Used for both creating and updating projects.
 * When id is provided, the matching project is updated; otherwise a new project is created.
 * The version field is required for updates to support optimistic locking.
 * Organization is referenced by organizationId when selecting an existing org,
 * or by organizationName when creating a new one.
 */
public record EditProjectInput(
        Long id,
        Integer version,
        String projectName,
        String name,
        String description,
        Long organizationId,
        String organizationName
) {
}
