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
package com.rreganjr.requel.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.*;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.UserStakeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CommandHandler decorator that checks authorization before delegating
 * to the next handler. Commands that implement {@link AuthorizableCommand}
 * declare their authorization requirement; this handler enforces it.
 * <p>
 * Placed in the handler chain between ExceptionMappingCommandHandler and
 * AnalysisInvokingCommandHandler, so authorization failures are mapped to
 * the appropriate HTTP response by the exception mapping layer.
 */
public class AuthorizingCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthorizingCommandHandler.class);

    private final CommandHandler delegate;

    public AuthorizingCommandHandler(CommandHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        if (command instanceof AuthorizableCommand authCmd) {
            checkAuthorization(authCmd);
        }
        return delegate.execute(command);
    }

    private void checkAuthorization(AuthorizableCommand command) {
        AuthorizationRequirement req = command.getAuthorizationRequirement();
        if (req == null) return;

        User user = command.getEditedBy();
        if (user == null) {
            throw new AuthorizationException("No user set on command for authorization check");
        }

        if (req instanceof RequiresSystemRole r) {
            if (!user.hasRole(r.roleType())) {
                throw new AuthorizationException(
                        "Requires role: " + r.roleType().getSimpleName());
            }
        } else if (req instanceof RequiresRolePermission r) {
            if (user instanceof com.rreganjr.requel.user.User requelUser) {
                boolean found = false;
                for (var role : requelUser.getUserRoles()) {
                    for (var perm : role.getAvailableUserRolePermissions()) {
                        if (role.hasUserRolePermission(perm)
                                && r.permissionName().equals(perm.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (!found) {
                    throw new AuthorizationException(
                            "Requires permission: " + r.permissionName());
                }
            } else {
                throw new AuthorizationException(
                        "Cannot check role permissions for user type: " + user.getClass().getName());
            }
        } else if (req instanceof RequiresStakeholderPermission r) {
            if (command instanceof ProjectScopedCommand psc) {
                try {
                    UserStakeholder stakeholder = psc.getProject().getUserStakeholder(user);
                    String permKey = r.entityType().getName() + "[" + r.permissionType() + "]";
                    boolean hasPermission = stakeholder.getStakeholderPermissions().stream()
                            .anyMatch(p -> permKey.equals(p.getPermissionKey()));
                    if (!hasPermission) {
                        throw new AuthorizationException(
                                "Requires stakeholder permission: "
                                + r.entityType().getSimpleName() + "[" + r.permissionType() + "]");
                    }
                } catch (AuthorizationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new AuthorizationException(
                            "User is not a stakeholder on the target project", e);
                }
            } else {
                throw new AuthorizationException(
                        "Stakeholder permission required but command "
                        + "does not provide project context");
            }
        }
        log.trace("Authorization check passed for {} by user {}",
                command.getClass().getSimpleName(), user.getUsername());
    }
}
