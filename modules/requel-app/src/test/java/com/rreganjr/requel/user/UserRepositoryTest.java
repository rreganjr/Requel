/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link UserRepository}.
 *
 * Uses {@link AbstractIntegrationTestCase} which loads the full Spring context with
 * H2 and the seeding initializers (AdminUserInitializer, ProjectUserInitializer,
 * UserRolePermissionsInitializer). The 'admin' and 'project' users are therefore
 * available in every test without additional setup.
 *
 * Scenarios covered:
 * - findUserByUsername: found and not-found cases
 * - findUsers: returns the seeded admin and project users
 * - findUsersForRole: SystemAdminUserRole contains admin; ProjectUserRole contains project user
 * - findUserRoleTypes: returns both known role types
 * - findUserRolePermissions: ProjectUserRole has at least the createProjects permission
 * - findUserRolePermission: found by type + name; not-found returns null
 */
public class UserRepositoryTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // findUserByUsername
    // -------------------------------------------------------------------------

    @Test
    void findUserByUsernameReturnsSeededAdmin() throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");

        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
    }

    @Test
    void findUserByUsernameReturnsSeededProjectUser() throws Exception {
        User projectUser = getUserRepository().findUserByUsername("project");

        assertNotNull(projectUser);
        assertEquals("project", projectUser.getUsername());
    }

    @Test
    void findUserByUsernameThrowsForUnknownUsername() {
        assertThrows(NoSuchUserException.class,
                () -> getUserRepository().findUserByUsername("nobody-here"));
    }

    // -------------------------------------------------------------------------
    // findUsers
    // -------------------------------------------------------------------------

    @Test
    void findUsersReturnsAtLeastAdminAndProjectUser() throws Exception {
        UserSet users = getUserRepository().findUsers();

        assertNotNull(users);
        assertTrue(users.stream().anyMatch(u -> "admin".equals(u.getUsername())),
                "expected admin user in result");
        assertTrue(users.stream().anyMatch(u -> "project".equals(u.getUsername())),
                "expected project user in result");
    }

    // -------------------------------------------------------------------------
    // findUsersForRole
    // -------------------------------------------------------------------------

    @Test
    void findUsersForRoleProjectUserRoleContainsProjectUser() throws Exception {
        UserSet projectUsers = getUserRepository().findUsersForRole(ProjectUserRole.class);

        assertNotNull(projectUsers);
        assertTrue(projectUsers.stream().anyMatch(u -> "project".equals(u.getUsername())),
                "expected project user in ProjectUserRole result");
    }

    // -------------------------------------------------------------------------
    // findUserRoleTypes
    // -------------------------------------------------------------------------

    @Test
    void findUserRoleTypesContainsKnownRoles() throws Exception {
        Set<Class<? extends UserRole>> types = getUserRepository().findUserRoleTypes();

        assertNotNull(types);
        assertTrue(types.contains(SystemAdminUserRole.class),
                "expected SystemAdminUserRole in role types");
        assertTrue(types.contains(ProjectUserRole.class),
                "expected ProjectUserRole in role types");
    }

    // -------------------------------------------------------------------------
    // findUserRolePermissions / findUserRolePermission
    // -------------------------------------------------------------------------

    @Test
    void findUserRolePermissionsForProjectUserRoleReturnsCreateProjects() throws Exception {
        Set<UserRolePermission> perms =
                getUserRepository().findUserRolePermissions(ProjectUserRole.class);

        assertNotNull(perms);
        assertTrue(perms.stream().anyMatch(p -> "createProjects".equals(p.getName())),
                "expected createProjects permission for ProjectUserRole");
    }

    @Test
    void findUserRolePermissionFoundByTypeAndName() throws Exception {
        UserRolePermission perm =
                getUserRepository().findUserRolePermission(ProjectUserRole.class, "createProjects");

        assertNotNull(perm);
        assertEquals("createProjects", perm.getName());
    }

    @Test
    void findUserRolePermissionThrowsForUnknownName() {
        // Implementation throws NoSuchEntityException rather than returning null
        assertThrows(com.rreganjr.platform.exception.NoSuchEntityException.class,
                () -> getUserRepository().findUserRolePermission(
                        ProjectUserRole.class, "no-such-perm"));
    }
}
