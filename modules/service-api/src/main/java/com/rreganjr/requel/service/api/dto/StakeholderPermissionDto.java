package com.rreganjr.requel.service.api.dto;

/**
 * A single stakeholder permission entry from the available permissions catalog.
 *
 * @param permissionKey    fully-qualified key (e.g. "com.rreganjr.requel.project.Goal[Edit]")
 * @param entityType       simple name of the entity type (e.g. "Goal", "Story")
 * @param permissionType   permission type name (e.g. "Edit", "Delete", "Grant")
 */
public record StakeholderPermissionDto(
        String permissionKey,
        String entityType,
        String permissionType
) {
}
