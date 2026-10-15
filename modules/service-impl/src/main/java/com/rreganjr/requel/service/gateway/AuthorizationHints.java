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
package com.rreganjr.requel.service.gateway;

import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresRolePermission;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermissionOrSystemAdminRole;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.platform.identity.Role;

/**
 * Renders a command's {@link AuthorizationRequirement} as the caller-facing authorization hint on
 * its {@link com.rreganjr.requel.gateway.CommandDescriptor} (issue #296).
 *
 * <p>The hint is informational: enforcement stays in {@code AuthorizingCommandHandler}. It follows
 * the stakeholder-permission notation the UI and {@code doc/architecture/AUTH_ARCH.md} use, an
 * entity type and a permission type, {@code Goal[Edit]}.
 *
 * <p>{@code AuthorizationHintsTest} renders every permitted subclass of the sealed interface, so a
 * new kind of requirement fails the build until it is given a rendering here.
 */
final class AuthorizationHints {

    /** The simple name of {@code user-jpa}'s administrator role, which this module cannot see. */
    static final String SYSTEM_ADMIN_ROLE = "SystemAdminUserRole";

    private AuthorizationHints() {
    }

    /** The hint for a requirement, or {@code null} for a command that declares none. */
    static String render(AuthorizationRequirement requirement) {
        if (requirement == null) {
            return null;
        }
        // Java 17: no pattern switch, so an if-chain. AuthorizationHintsTest walks
        // AuthorizationRequirement's permitted subclasses, so a new kind of requirement without a
        // branch here fails that test rather than shipping as an exception at startup.
        if (requirement instanceof RequiresStakeholderPermission r) {
            return permission(r.entityType(), r.permissionType());
        }
        if (requirement instanceof RequiresStakeholderPermissionOrSystemAdminRole r) {
            return permission(r.entityType(), r.permissionType()) + " or " + role(r.roleType());
        }
        if (requirement instanceof RequiresSystemRole r) {
            return role(r.roleType());
        }
        if (requirement instanceof RequiresRolePermission r) {
            return r.permissionName() + " role permission";
        }
        throw new IllegalArgumentException("No hint rendering for " + requirement.getClass().getName());
    }

    private static String permission(Class<?> entityType, String permissionType) {
        return entityType.getSimpleName() + "[" + permissionType + "]";
    }

    /** A role by name: the administrator role reads as a phrase, any other by its type name. */
    static String role(Class<? extends Role> roleType) {
        String name = roleType.getSimpleName();
        return SYSTEM_ADMIN_ROLE.equals(name) ? "system administrator" : name;
    }
}
