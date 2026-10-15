/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
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

    /**
     * Satisfied either by the stakeholder permission (exactly as
     * {@link RequiresStakeholderPermission}) or by holding the given administrator role, which
     * needs no stakeholder membership on the target project at all. The command must
     * implement ProjectScopedCommand to provide the project context for the stakeholder
     * branch.
     *
     * <p>
     * Deliberately narrow (issue #256): system administrators do <em>not</em> generally
     * satisfy stakeholder permissions - {@code AuthorizationIT} pins that an admin
     * without {@code Goal[Edit]} cannot edit a goal - so this variant exists for the one
     * operation where a system-level capability is the point: deleting a whole project
     * during ops cleanup, where the admin is by definition not a stakeholder on the
     * rubbish being removed. Use it only where that reasoning applies; reach for
     * {@link RequiresStakeholderPermission} everywhere else.
     *
     * @param entityType     the domain entity type (e.g. Project.class)
     * @param permissionType the permission type string ("Edit", "Delete", "Grant")
     * @param roleType       the administrator role that also satisfies this requirement
     */
    record RequiresStakeholderPermissionOrSystemAdminRole(Class<?> entityType, String permissionType,
            Class<? extends Role> roleType) implements AuthorizationRequirement {
    }
}
