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
package com.rreganjr.requel.service.auth;

import org.springframework.stereotype.Component;

import com.rreganjr.platform.identity.password.PasswordHasher;
import com.rreganjr.requel.user.User;

/**
 * Checks a sign-in password so that every failure costs the same (issue #448). A known user's
 * password is hashed to compare it; without this, an unknown username (or an assistant account,
 * which never signs in, #260) would fail at once and the timing would show which usernames exist.
 * Those cases hash the password against a fixed dummy value, with the same algorithm and
 * iterations as a real password.
 */
@Component
public class PasswordCheck {

    /** Mirrors {@code UserImpl.isPassword}: a longer password is refused without hashing. */
    private static final int MAX_CHECKED_LENGTH = 128;

    private static final String DUMMY_SALT = PasswordHasher.generateSalt();

    /**
     * @param user
     *            the account the username names, or null for none
     * @return whether the password is the user's
     */
    public boolean matches(User user, String password) {
        if (user == null || com.rreganjr.platform.identity.User.isAssistant(user)) {
            dummyCheck(password);
            return false;
        }
        return user.isPassword(password);
    }

    /** The work a real check does, with a result nobody reads. */
    void dummyCheck(String password) {
        if (password != null && password.length() <= MAX_CHECKED_LENGTH) {
            PasswordHasher.hash(password, PasswordHasher.PREFERRED_ALGORITHM, DUMMY_SALT,
                    PasswordHasher.PREFERRED_ITERATIONS);
        }
    }
}
