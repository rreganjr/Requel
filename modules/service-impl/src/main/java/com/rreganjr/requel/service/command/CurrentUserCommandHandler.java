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
package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Command handler decorator that resolves the current authenticated user from
 * Spring Security and sets {@code editedBy} on commands that support it.
 * This eliminates the need for callers to manually set editedBy before dispatch.
 *
 * <p>Wraps the outermost handler in the chain so editedBy is set before
 * authorization checks or retries.</p>
 *
 * <p>If no one is signed in (initializers, background jobs, the dev-only reset endpoints), the
 * handler skips user resolution and delegates directly; the command runs as an internal call.
 * If someone <em>is</em> signed in but their account can't be resolved, the command is refused
 * (#445): a lookup failure must never turn a request into an internal call.</p>
 */
public class CurrentUserCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserCommandHandler.class);

    private final CommandHandler delegate;
    private final CurrentUserResolver currentUserResolver;

    public CurrentUserCommandHandler(CommandHandler delegate, CurrentUserResolver currentUserResolver) {
        this.delegate = delegate;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        // Only resolve the user when the command actually needs it
        if (command instanceof EditCommand editCmd && editCmd.getEditedBy() == null) {
            resolveAndSet(user -> editCmd.setEditedBy(user));
        } else if (command instanceof EditUserCommand userCmd && userCmd.getEditedBy() == null) {
            // Only inject editedBy from the security context when not already set (e.g. REST API
            // dispatch where no caller sets editedBy; tests set it explicitly and must not be overridden).
            resolveAndSet(user -> userCmd.setEditedBy(user));
        }
        return delegate.execute(command);
    }

    private void resolveAndSet(java.util.function.Consumer<com.rreganjr.requel.user.User> setter) {
        if (!isSignedIn(SecurityContextHolder.getContext().getAuthentication())) {
            log.debug("No signed-in user; running as an internal call");
            return;
        }
        com.rreganjr.requel.user.User user;
        try {
            user = currentUserResolver.resolve();
        } catch (Exception e) {
            log.warn("Refusing a command: the signed-in account could not be resolved: {}", e.getMessage());
            throw new AuthorizationException("The signed-in account could not be found.");
        }
        if (user == null) {
            throw new AuthorizationException("The signed-in account could not be found.");
        }
        setter.accept(user);
    }

    private static boolean isSignedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
