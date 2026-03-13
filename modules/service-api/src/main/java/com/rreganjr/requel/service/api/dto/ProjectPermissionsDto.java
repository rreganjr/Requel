package com.rreganjr.requel.service.api.dto;

import java.util.Map;
import java.util.Set;

/**
 * Permissions for the current user within a specific project.
 * The permissions map uses simplified entity type names (e.g., "Goal", "Story")
 * as keys and sets of permission types ("Edit", "Delete", "Grant") as values.
 */
public record ProjectPermissionsDto(
        boolean isStakeholder,
        boolean canCreateProjects,
        Map<String, Set<String>> permissions
) {}
