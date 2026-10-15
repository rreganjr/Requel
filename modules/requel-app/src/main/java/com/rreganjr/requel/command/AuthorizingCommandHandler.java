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
import com.rreganjr.platform.command.CascadeAuthorizable;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.*;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

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

    /**
     * Issue #75: the commands this thread is executing that passed their check, innermost first. A
     * {@link CascadeAuthorizable} step borrows its authorizing command's check only while that
     * command is here.
     */
    private static final ThreadLocal<Deque<AuthorizableCommand>> AUTHORIZED =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        if (command instanceof CascadeAuthorizable step && step.getAuthorizingCommand() != null) {
            // #75: a step of an authorized operation (a delete's leftovers, what it alone owns)
            // is covered by that operation's permission - checked, not skipped.
            requireExecutingAuthorizer(command, step.getAuthorizingCommand());
            return delegate.execute(command);
        }
        if (command instanceof AuthorizableCommand authCmd) {
            checkAuthorization(authCmd);
            Deque<AuthorizableCommand> authorized = AUTHORIZED.get();
            authorized.push(authCmd);
            try {
                return delegate.execute(command);
            } finally {
                authorized.pop();
                if (authorized.isEmpty()) {
                    AUTHORIZED.remove();
                }
            }
        }
        return delegate.execute(command);
    }

    /**
     * #75: a cascade step is accepted only while the command that authorizes it is executing on
     * this thread, having passed its own check, for the same user.
     */
    private void requireExecutingAuthorizer(Command step, AuthorizableCommand authorizer) {
        boolean executing = false;
        for (AuthorizableCommand running : AUTHORIZED.get()) {
            // Commands are Spring proxies; the parent hands on itself (the target) from inside
            // execute(), while the handler was given the proxy.
            if (running == authorizer
                    || org.springframework.aop.framework.AopProxyUtils.getSingletonTarget(running)
                            == authorizer) {
                executing = true;
                break;
            }
        }
        if (!executing) {
            throw new AuthorizationException(step.getClass().getSimpleName()
                    + " is a step of " + authorizer.getClass().getSimpleName()
                    + ", which is not executing");
        }
        User stepUser = step instanceof EditCommand edit ? edit.getEditedBy() : null;
        User authorizerUser = authorizer.getEditedBy();
        if (stepUser != null && authorizerUser != null
                && !Objects.equals(stepUser.getId(), authorizerUser.getId())) {
            throw new AuthorizationException(step.getClass().getSimpleName()
                    + " runs as a different user from " + authorizer.getClass().getSimpleName());
        }
        log.trace("{} authorized as a step of {}", step.getClass().getSimpleName(),
                authorizer.getClass().getSimpleName());
    }

    private void checkAuthorization(AuthorizableCommand command) {
        AuthorizationRequirement req = command.getAuthorizationRequirement();
        if (req == null) return;

        User user = command.getEditedBy();
        if (user == null) {
            // Null editedBy means bootstrap / internal call (e.g. data initializers).
            // The execute() body in each command validates any further constraints.
            log.debug("Skipping authorization check — editedBy is null (bootstrap/internal)");
            return;
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
            checkStakeholderPermission(command, user, r.entityType(), r.permissionType());
        } else if (req instanceof RequiresStakeholderPermissionOrSystemAdminRole r) {
            // The administrator role is an alternative to stakeholder membership, not an addition to it
            // (issue #256): an administrator cleaning up projects is by definition not a
            // stakeholder on them. Everyone else falls through to the identical stakeholder check.
            if (!user.hasRole(r.roleType())) {
                checkStakeholderPermission(command, user, r.entityType(), r.permissionType());
            }
        }
        log.trace("Authorization check passed for {} by user {}",
                command.getClass().getSimpleName(), user.getUsername());
    }

    /**
     * The stakeholder-permission check shared by {@code RequiresStakeholderPermission} and the
     * stakeholder branch of {@code RequiresStakeholderPermissionOrSystemAdminRole}. One copy on
     * purpose: two would drift, and a stale authorization check is worse than a verbose one.
     */
    private void checkStakeholderPermission(AuthorizableCommand command, User user,
            Class<?> entityType, String permissionType) {
        if (!(command instanceof ProjectScopedCommand psc)) {
            throw new AuthorizationException(
                    "Stakeholder permission required but command "
                    + "does not provide project context");
        }
        StakeholderAuthorizationChecker.require(psc.getProject(), user, entityType,
                permissionType);
    }
}
