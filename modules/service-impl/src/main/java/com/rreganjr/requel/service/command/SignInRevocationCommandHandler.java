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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.service.auth.SignInRevocation;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * After an admin reset of another user's password ({@link EditUserCommand#isPasswordReset()}),
 * ends that user's other sign-ins and revokes their personal access tokens (issue #446). The
 * command itself lives in user-jpa, which can't reach the token or OAuth tables, so the follow-up
 * runs here, after the command has committed.
 *
 * <p>A failure here is logged, not thrown: the password has already changed, and the user's old
 * login and OAuth access tokens are refused anyway by the "password changed at" check.
 */
public class SignInRevocationCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SignInRevocationCommandHandler.class);

    private final CommandHandler delegate;
    private final SignInRevocation signInRevocation;

    public SignInRevocationCommandHandler(CommandHandler delegate, SignInRevocation signInRevocation) {
        this.delegate = delegate;
        this.signInRevocation = signInRevocation;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        T result = delegate.execute(command);
        if (result instanceof EditUserCommand edit && edit.isPasswordReset() && edit.getUser() != null) {
            try {
                signInRevocation.afterPasswordChange(edit.getUser(), true);
            } catch (RuntimeException e) {
                log.error("Password reset for user '{}' saved, but ending their other sign-ins failed: {}",
                        edit.getUser().getUsername(), e.getMessage(), e);
            }
        }
        return result;
    }
}
