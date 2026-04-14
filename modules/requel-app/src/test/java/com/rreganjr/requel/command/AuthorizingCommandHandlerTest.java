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
package com.rreganjr.requel.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.*;
import com.rreganjr.platform.identity.Role;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthorizingCommandHandler}.
 *
 * Pure Mockito test — no Spring context. The handler is a decorator constructed
 * directly. Local marker interfaces combine {@link AuthorizableCommand} with
 * {@link ProjectScopedCommand} for the stakeholder-permission branch.
 *
 * Scenarios covered:
 * - Non-AuthorizableCommand: bypasses auth, delegates directly
 * - AuthorizableCommand with null requirement: no auth check, delegates
 * - AuthorizableCommand with null user: skips authorization (bootstrap/internal calls permitted)
 * - RequiresSystemRole — user has role: passes, delegates
 * - RequiresSystemRole — user lacks role: throws AuthorizationException
 * - RequiresRolePermission — requel.user.User with matching permission: passes
 * - RequiresRolePermission — requel.user.User, permission name not matched: throws
 * - RequiresRolePermission — platform.identity.User (not requel.user.User): throws
 * - RequiresStakeholderPermission on ProjectScopedCommand — matching key: passes
 * - RequiresStakeholderPermission — stakeholder lacks permission: throws
 * - RequiresStakeholderPermission — getUserStakeholder throws: throws AuthorizationException
 * - RequiresStakeholderPermission on non-ProjectScopedCommand: throws
 */
class AuthorizingCommandHandlerTest {

    // Local interfaces for multi-interface commands
    interface AuthCmd extends AuthorizableCommand {}
    interface ProjectAuthCmd extends AuthorizableCommand, ProjectScopedCommand {}

    private CommandHandler delegate;
    private AuthorizingCommandHandler handler;

    @BeforeEach
    void setUp() {
        delegate = mock(CommandHandler.class);
        handler = new AuthorizingCommandHandler(delegate);
    }

    // -------------------------------------------------------------------------
    // Non-AuthorizableCommand — bypass
    // -------------------------------------------------------------------------

    @Test
    void nonAuthorizableCommandDelegatesWithoutAuthCheck() throws Exception {
        Command cmd = mock(Command.class);

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    // -------------------------------------------------------------------------
    // Null requirement — skip check
    // -------------------------------------------------------------------------

    @Test
    void nullRequirementDelegatesWithoutException() throws Exception {
        AuthCmd cmd = mock(AuthCmd.class);
        when(cmd.getAuthorizationRequirement()).thenReturn(null);

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    // -------------------------------------------------------------------------
    // Null user — skip authorization (bootstrap/internal calls permitted through)
    // -------------------------------------------------------------------------

    @Test
    void nullUserSkipsAuthorizationAndDelegates() throws Exception {
        AuthCmd cmd = mock(AuthCmd.class);
        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresSystemRole(SystemAdminRole.class));
        when(cmd.getEditedBy()).thenReturn(null);

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    // -------------------------------------------------------------------------
    // RequiresSystemRole
    // -------------------------------------------------------------------------

    @Test
    void requiresSystemRolePassesWhenUserHasRole() throws Exception {
        AuthCmd cmd = mock(AuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);
        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresSystemRole(SystemAdminRole.class));
        when(cmd.getEditedBy()).thenReturn(user);
        when(user.hasRole(SystemAdminRole.class)).thenReturn(true);
        when(user.getUsername()).thenReturn("admin");

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    @Test
    void requiresSystemRoleThrowsWhenUserLacksRole() {
        AuthCmd cmd = mock(AuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);
        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresSystemRole(SystemAdminRole.class));
        when(cmd.getEditedBy()).thenReturn(user);
        when(user.hasRole(SystemAdminRole.class)).thenReturn(false);

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class);
    }

    // -------------------------------------------------------------------------
    // RequiresRolePermission
    // -------------------------------------------------------------------------

    @Test
    void requiresRolePermissionPassesWhenRequelUserHasPermission() throws Exception {
        AuthCmd cmd = mock(AuthCmd.class);
        com.rreganjr.requel.user.User requelUser = mock(com.rreganjr.requel.user.User.class);
        UserRole role = mock(UserRole.class);
        UserRolePermission perm = mock(UserRolePermission.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresRolePermission("createProjects"));
        when(cmd.getEditedBy()).thenReturn(requelUser);
        when(requelUser.getUserRoles()).thenReturn(Set.of(role));
        when(role.getAvailableUserRolePermissions()).thenReturn(Set.of(perm));
        when(role.hasUserRolePermission(perm)).thenReturn(true);
        when(perm.getName()).thenReturn("createProjects");
        when(requelUser.getUsername()).thenReturn("alice");

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    @Test
    void requiresRolePermissionThrowsWhenPermissionNameNotMatched() {
        AuthCmd cmd = mock(AuthCmd.class);
        com.rreganjr.requel.user.User requelUser = mock(com.rreganjr.requel.user.User.class);
        UserRole role = mock(UserRole.class);
        UserRolePermission perm = mock(UserRolePermission.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresRolePermission("createProjects"));
        when(cmd.getEditedBy()).thenReturn(requelUser);
        when(requelUser.getUserRoles()).thenReturn(Set.of(role));
        when(role.getAvailableUserRolePermissions()).thenReturn(Set.of(perm));
        when(role.hasUserRolePermission(perm)).thenReturn(true);
        when(perm.getName()).thenReturn("deleteProjects"); // different permission

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void requiresRolePermissionThrowsForPlatformIdentityUser() {
        AuthCmd cmd = mock(AuthCmd.class);
        // platform.identity.User — not requel.user.User
        com.rreganjr.platform.identity.User platformUser = mock(com.rreganjr.platform.identity.User.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresRolePermission("createProjects"));
        when(cmd.getEditedBy()).thenReturn(platformUser);

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class);
    }

    // -------------------------------------------------------------------------
    // RequiresStakeholderPermission
    // -------------------------------------------------------------------------

    @Test
    void requiresStakeholderPermissionPassesWhenKeyMatches() throws Exception {
        ProjectAuthCmd cmd = mock(ProjectAuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);
        Project project = mock(Project.class);
        UserStakeholder stakeholder = mock(UserStakeholder.class);
        StakeholderPermission perm = mock(StakeholderPermission.class);

        // permission key: entityType.getName() + "[" + permissionType + "]"
        String expectedKey = com.rreganjr.requel.project.Goal.class.getName() + "[Edit]";

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresStakeholderPermission(
                        com.rreganjr.requel.project.Goal.class, "Edit"));
        when(cmd.getEditedBy()).thenReturn(user);
        when(cmd.getProject()).thenReturn(project);
        when(project.getUserStakeholder(user)).thenReturn(stakeholder);
        when(stakeholder.getStakeholderPermissions()).thenReturn(Set.of(perm));
        when(perm.getPermissionKey()).thenReturn(expectedKey);
        when(user.getUsername()).thenReturn("alice");

        handler.execute(cmd);

        verify(delegate).execute(cmd);
    }

    @Test
    void requiresStakeholderPermissionThrowsWhenPermissionKeyNotFound() {
        ProjectAuthCmd cmd = mock(ProjectAuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);
        Project project = mock(Project.class);
        UserStakeholder stakeholder = mock(UserStakeholder.class);
        StakeholderPermission perm = mock(StakeholderPermission.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresStakeholderPermission(
                        com.rreganjr.requel.project.Goal.class, "Edit"));
        when(cmd.getEditedBy()).thenReturn(user);
        when(cmd.getProject()).thenReturn(project);
        when(project.getUserStakeholder(user)).thenReturn(stakeholder);
        when(stakeholder.getStakeholderPermissions()).thenReturn(Set.of(perm));
        when(perm.getPermissionKey()).thenReturn("com.rreganjr.requel.project.Actor[Edit]"); // wrong type

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void requiresStakeholderPermissionThrowsWhenGetUserStakeholderThrows() {
        ProjectAuthCmd cmd = mock(ProjectAuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);
        Project project = mock(Project.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresStakeholderPermission(
                        com.rreganjr.requel.project.Goal.class, "Edit"));
        when(cmd.getEditedBy()).thenReturn(user);
        when(cmd.getProject()).thenReturn(project);
        when(project.getUserStakeholder(user))
                .thenThrow(new RuntimeException("user is not a stakeholder"));

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("not a stakeholder");
    }

    @Test
    void requiresStakeholderPermissionThrowsWhenCommandIsNotProjectScoped() {
        // AuthorizableCommand but NOT ProjectScopedCommand
        AuthCmd cmd = mock(AuthCmd.class);
        com.rreganjr.platform.identity.User user = mock(com.rreganjr.platform.identity.User.class);

        when(cmd.getAuthorizationRequirement())
                .thenReturn(new RequiresStakeholderPermission(
                        com.rreganjr.requel.project.Goal.class, "Edit"));
        when(cmd.getEditedBy()).thenReturn(user);

        assertThatThrownBy(() -> handler.execute(cmd))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("does not provide project context");
    }

    // -------------------------------------------------------------------------
    // Dummy Role class for RequiresSystemRole tests
    // -------------------------------------------------------------------------

    /** Concrete Role subtype for use in RequiresSystemRole constructor. */
    static class SystemAdminRole implements Role {
        @Override public String getName() { return "SystemAdminUserRole"; }
    }
}
