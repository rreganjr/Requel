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
package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.identity.UserRoleImportDraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserRoleImportXmlMapper {

    public List<UserRoleImportDraft> toDrafts(UserRoleImportXml xml) {
        if (xml == null) {
            return List.of();
        }
        List<UserRoleImportDraft> drafts = new ArrayList<>();
        Set<String> fallbackPermissions = new HashSet<>();

        for (Object roleObj : xml.getRoles()) {
            if (roleObj instanceof UserRoleImportXml.ProjectUserRoleXml projectRole) {
                drafts.add(new UserRoleImportDraft(
                        "com.rreganjr.requel.project.ProjectUserRole",
                        permissionNames(projectRole.getUserPermissions())));
            } else if (roleObj instanceof UserRoleImportXml.SystemAdminUserRoleXml) {
                drafts.add(new UserRoleImportDraft(
                        "com.rreganjr.requel.user.impl.SystemAdminUserRole",
                        Set.of()));
            } else if (roleObj instanceof UserRoleImportXml.UserPermissionContainer container) {
                // Legacy format: userPermissions directly under userRoles
                fallbackPermissions.addAll(permissionNames(container));
            } else if (roleObj instanceof UserRoleImportXml.UserPermissionXml perm) {
                if (perm.getName() != null) {
                    fallbackPermissions.add(perm.getName());
                }
            } else {
                // unknown role type; skip
            }
        }
        // Legacy safeguard: if no roles parsed, assume ProjectUserRole with any collected permissions
        if (drafts.isEmpty()) {
            drafts.add(new UserRoleImportDraft(
                    "com.rreganjr.requel.project.ProjectUserRole",
                    fallbackPermissions));
        }
        return drafts;
    }

    private Set<String> permissionNames(UserRoleImportXml.UserPermissionContainer container) {
        Set<String> names = new HashSet<>();
        if (container != null) {
            container.getPermissions().forEach(p -> {
                if (p.getName() != null) {
                    names.add(p.getName());
                }
            });
        }
        return names;
    }
}
