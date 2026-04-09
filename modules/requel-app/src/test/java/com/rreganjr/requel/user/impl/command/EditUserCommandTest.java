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
package com.rreganjr.requel.user.impl.command;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.validator.EntityValidationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link EditUserCommand}.
 *
 * {@code EditUserCommand} is the single command covering user creation, profile
 * update, password change, and role assignment. There are no separate
 * {@code DeleteUser} or {@code ChangePassword} commands in this codebase;
 * password changes go through {@code EditUser} via setPassword/setRepassword,
 * and user deletion is not yet implemented.
 *
 * Authorization rules enforced by the command:
 * - Only admins ({@code SystemAdminUserRole}) or the system bootstrap (null editedBy)
 *   can create new user accounts.
 * - Non-admins can edit only their own account.
 * - Only admins can assign or revoke roles.
 *
 * Every user must have at least one role ({@code @Size(min=1)} on userRoles).
 * With only {@code ProjectUserRole} and {@code SystemAdminUserRole} active,
 * "starts without role X" tests must start with the other role.
 */
public class EditUserCommandTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a fresh user with {@code ProjectUserRole} as the initial role.
     * All test users need at least one role ({@code @Size(min=1)} on userRoles).
     */
    private User createUser(String username, String password, String name) throws Exception {
        return createUserWithRole(username, password, name, ProjectUserRole.class.getSimpleName());
    }

    /**
     * Creates a fresh user with the specified role as the sole initial role.
     * Use this when you need to start without a specific role (e.g., testing
     * that admin can grant ProjectUserRole to a user who doesn't have it yet).
     */
    private User createUserWithRole(String username, String password, String name,
            String roleName) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword(password);
        cmd.setRepassword(password);
        cmd.setName(name);
        cmd.setEmailAddress(username + "@example.com");
        // Empty phone number is valid (the constraint allows empty or 10-digit format)
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("TestOrg");
        cmd.addUserRoleName(roleName);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUser();
    }

    // -------------------------------------------------------------------------
    // Create user
    // -------------------------------------------------------------------------

    @Test
    public void createUser() throws Exception {
        long ts = System.currentTimeMillis();
        String username = "testuser-" + ts;

        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword("s3cr3t!");
        cmd.setRepassword("s3cr3t!");
        cmd.setName("Test User");
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("NewOrg-" + ts);
        cmd.addUserRoleName(ProjectUserRole.class.getSimpleName());
        cmd = getCommandHandler().execute(cmd);

        User created = cmd.getUser();
        assertNotNull(created, "user should have been created");
        assertEquals(username, created.getUsername(), "username should match");
        assertEquals("Test User", created.getName(), "name should match");
        assertEquals(username + "@example.com", created.getEmailAddress(),
                "email should match");
        assertTrue(created.isPassword("s3cr3t!"), "password should authenticate correctly");

        // Verify findable in repository
        User found = getUserRepository().findUserByUsername(username);
        assertNotNull(found, "created user should be findable by username");
    }

    // -------------------------------------------------------------------------
    // Edit user profile
    // -------------------------------------------------------------------------

    @Test
    public void editOwnProfile() throws Exception {
        long ts = System.currentTimeMillis();
        User user = createUser("profile-user-" + ts, "pass123!", "Original Name");

        // User edits their own account
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(user);
        cmd.setUser(user);
        cmd.setUsername(user.getUsername());
        cmd.setName("Updated Name");
        cmd.setEmailAddress("updated-" + ts + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("UpdatedOrg");
        cmd = getCommandHandler().execute(cmd);

        User updated = cmd.getUser();
        assertEquals("Updated Name", updated.getName(), "name should be updated");
        assertEquals("updated-" + ts + "@example.com", updated.getEmailAddress(),
                "email should be updated");
    }

    // -------------------------------------------------------------------------
    // Password change (via EditUser)
    // -------------------------------------------------------------------------

    @Test
    public void changePassword() throws Exception {
        long ts = System.currentTimeMillis();
        User user = createUser("passwd-user-" + ts, "oldPass1!", "Password User");
        assertTrue(user.isPassword("oldPass1!"), "initial password should authenticate");

        // User changes their own password
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(user);
        cmd.setUser(user);
        cmd.setUsername(user.getUsername());
        cmd.setName(user.getName());
        cmd.setEmailAddress(user.getEmailAddress());
        cmd.setOrganizationName(
                user.getOrganization() != null ? user.getOrganization().getName() : "TestOrg");
        cmd.setPassword("newPass2!");
        cmd.setRepassword("newPass2!");
        cmd = getCommandHandler().execute(cmd);

        User updated = cmd.getUser();
        assertTrue(updated.isPassword("newPass2!"), "new password should authenticate");
        assertFalse(updated.isPassword("oldPass1!"), "old password should no longer work");
    }

    @Test
    public void passwordMismatchIsRejected() throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");

        assertThrows(EntityValidationException.class, () -> {
            EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
            cmd.setEditedBy(admin);
            cmd.setUsername("mismatch-" + ts);
            cmd.setPassword("passA1!");
            cmd.setRepassword("passB2!");
            cmd.setName("Mismatch User");
            cmd.setEmailAddress("mismatch@example.com");
            cmd.setOrganizationName("TestOrg");
            cmd.addUserRoleName(ProjectUserRole.class.getSimpleName());
            getCommandHandler().execute(cmd);
        }, "mismatched password/repassword should be rejected");
    }

    // -------------------------------------------------------------------------
    // Role assignment (admin only)
    // -------------------------------------------------------------------------

    @Test
    public void adminGrantsProjectUserRole() throws Exception {
        long ts = System.currentTimeMillis();
        // Start with SystemAdminUserRole so we can verify ProjectUserRole is not present.
        // Every user needs at least one role, and only two are active in this system.
        User user = createUserWithRole("role-user-" + ts, "pass123!", "Role User",
                SystemAdminUserRole.class.getSimpleName());
        assertFalse(user.hasRole(ProjectUserRole.class),
                "user should not have ProjectUserRole before grant");

        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUser(user);
        cmd.setUsername(user.getUsername());
        cmd.setName(user.getName());
        cmd.setEmailAddress(user.getEmailAddress());
        cmd.setOrganizationName(
                user.getOrganization() != null ? user.getOrganization().getName() : "TestOrg");
        // Grant ProjectUserRole; preserve the existing SystemAdminUserRole
        cmd.addUserRoleName(SystemAdminUserRole.class.getSimpleName());
        cmd.addUserRoleName(ProjectUserRole.class.getSimpleName());
        cmd = getCommandHandler().execute(cmd);

        User updated = cmd.getUser();
        assertTrue(updated.hasRole(ProjectUserRole.class),
                "user should have ProjectUserRole after admin grant");
    }

    @Test
    public void adminGrantsSystemAdminRole() throws Exception {
        long ts = System.currentTimeMillis();
        // Start with ProjectUserRole so we can verify SystemAdminUserRole is not present.
        User user = createUser("admin-role-user-" + ts, "pass123!", "Admin Role User");
        assertFalse(user.hasRole(SystemAdminUserRole.class),
                "user should not have SystemAdminUserRole before grant");

        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUser(user);
        cmd.setUsername(user.getUsername());
        cmd.setName(user.getName());
        cmd.setEmailAddress(user.getEmailAddress());
        cmd.setOrganizationName(
                user.getOrganization() != null ? user.getOrganization().getName() : "TestOrg");
        // Grant SystemAdminUserRole; preserve the existing ProjectUserRole
        cmd.addUserRoleName(ProjectUserRole.class.getSimpleName());
        cmd.addUserRoleName(SystemAdminUserRole.class.getSimpleName());
        cmd = getCommandHandler().execute(cmd);

        User updated = cmd.getUser();
        assertTrue(updated.hasRole(SystemAdminUserRole.class),
                "user should have SystemAdminUserRole after admin grant");
    }

    // -------------------------------------------------------------------------
    // Authorization enforcement
    // -------------------------------------------------------------------------

    @Test
    public void nonAdminCannotCreateUser() throws Exception {
        long ts = System.currentTimeMillis();
        // 'project' has ProjectUserRole only — not SystemAdminUserRole
        User projectUser = getUserRepository().findUserByUsername("project");

        assertThrows(AuthorizationException.class, () -> {
            EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
            cmd.setEditedBy(projectUser);
            cmd.setUsername("unauthorized-" + ts);
            cmd.setPassword("pass123!");
            cmd.setRepassword("pass123!");
            cmd.setName("Unauthorized User");
            cmd.setEmailAddress("unauthorized@example.com");
            cmd.setOrganizationName("TestOrg");
            cmd.addUserRoleName(ProjectUserRole.class.getSimpleName());
            getCommandHandler().execute(cmd);
        }, "non-admin should not be able to create a new user account");
    }

    @Test
    public void nonAdminCannotEditOtherUser() throws Exception {
        long ts = System.currentTimeMillis();
        User target = createUser("edit-target-" + ts, "pass123!", "Edit Target");
        User projectUser = getUserRepository().findUserByUsername("project");

        assertThrows(AuthorizationException.class, () -> {
            EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
            cmd.setEditedBy(projectUser);
            cmd.setUser(target);
            cmd.setUsername(target.getUsername());
            cmd.setName("Hijacked Name");
            cmd.setEmailAddress("hijacked@example.com");
            cmd.setOrganizationName("TestOrg");
            getCommandHandler().execute(cmd);
        }, "non-admin should not be able to edit another user's account");
    }
}
