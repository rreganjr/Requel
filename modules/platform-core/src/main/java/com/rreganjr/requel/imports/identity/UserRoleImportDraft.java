/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.imports.identity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserRoleImportDraft {
    private final String roleType;
    private final Set<String> permissionNames;

    public UserRoleImportDraft(String roleType, Set<String> permissionNames) {
        this.roleType = roleType;
        this.permissionNames = permissionNames == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(permissionNames));
    }

    public String getRoleType() {
        return roleType;
    }

    public Set<String> getPermissionNames() {
        return permissionNames;
    }
}
