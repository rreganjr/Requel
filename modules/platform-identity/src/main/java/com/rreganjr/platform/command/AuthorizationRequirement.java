package com.rreganjr.platform.command;

import com.rreganjr.platform.identity.Role;

/**
 * Sealed interface representing the authorization requirement for a command.
 * Used by AuthorizingCommandHandler to check permissions before execution.
 * Each variant maps to a different authorization pattern in the domain.
 */
public sealed interface AuthorizationRequirement {

    /**
     * User must have the specified system role (e.g. SystemAdminUserRole).
     */
    record RequiresSystemRole(Class<? extends Role> roleType)
            implements AuthorizationRequirement {
    }

    /**
     * User must have the specified role-level permission (e.g. "createProjects").
     */
    record RequiresRolePermission(String permissionName)
            implements AuthorizationRequirement {
    }

    /**
     * User must be a stakeholder on the target project with the specified permission.
     * The command must also implement ProjectScopedCommand to provide the project context.
     *
     * @param entityType     the domain entity type (e.g. Goal.class, Actor.class)
     * @param permissionType the permission type string ("Edit", "Delete", "Grant")
     */
    record RequiresStakeholderPermission(Class<?> entityType, String permissionType)
            implements AuthorizationRequirement {
    }
}
