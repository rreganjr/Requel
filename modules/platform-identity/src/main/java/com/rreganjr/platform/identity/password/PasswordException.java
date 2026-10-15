/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.platform.identity.password;

import com.rreganjr.platform.exception.RequelException;

/**
 * Signals problems encountered while hashing or salting user passwords.
 */
public class PasswordException extends RequelException {

    public static final String MSG_PROBLEM_ENCRYPTING_PASSWORD = "There was a problem encrypting the user's password.";
    public static final String MSG_PROBLEM_ENCRYPTING_PASSWORD_WITH_MESSAGE = "There was a problem encrypting the user's password: %s";
    public static final String MSG_PROBLEM_GENERATING_PASSWORD_SALT = "There was a problem generating the password salt value.";
    public static final String MSG_BAD_ALGORITHM_NAME = "The supplied algorithm name '%s' is not a supported SecretKeyFactory or MessageDigest algorithm name.";

    public static PasswordException problemEncryptingPassword(Exception e) {
        return new PasswordException(e, MSG_PROBLEM_ENCRYPTING_PASSWORD);
    }

    public static PasswordException problemEncryptingPassword(String message) {
        return new PasswordException(MSG_PROBLEM_ENCRYPTING_PASSWORD_WITH_MESSAGE, message);
    }

    public static PasswordException badAlgorithmName(String badAlgorithmName) {
        return new PasswordException(MSG_BAD_ALGORITHM_NAME, badAlgorithmName);
    }

    public static PasswordException problemGeneratingPasswordSalt(Exception e) {
        return new PasswordException(e, MSG_PROBLEM_GENERATING_PASSWORD_SALT);
    }

    protected PasswordException(String format, Object... args) {
        super(format, args);
    }

    protected PasswordException(Throwable cause, String format, Object... args) {
        super(cause, format, args);
    }
}
