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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.service.api.CommandResult.FieldViolation;
import com.rreganjr.requel.service.api.dto.ChangePasswordRequest;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Issue #381: a signed-in user changes their own password, which also clears "must change
 * password". The current password is required (unlike {@code EditUser}), and the new one must
 * differ from it.
 */
@Service
public class PasswordChangeService {

    /** {@code UserImpl.MAX_PASSWORD_LENGTH}: the only bound the server puts on a password. */
    static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository userRepository;

    public PasswordChangeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the user, saved with the new password and the flag cleared
     * @throws PasswordChangeRefusedException naming each field that was refused; nothing changes
     */
    @Transactional
    public User changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findUserByUsername(username);
        String current = request.currentPassword();
        String next = request.newPassword();
        List<FieldViolation> violations = new ArrayList<>();

        boolean currentOk = current != null && user.isPassword(current);
        if (!currentOk) {
            violations.add(new FieldViolation("currentPassword", "Current password is incorrect."));
        }
        if (next == null || next.isBlank()) {
            violations.add(new FieldViolation("newPassword", "New password is required."));
        } else if (next.length() > MAX_PASSWORD_LENGTH) {
            violations.add(new FieldViolation("newPassword",
                    "New password can be at most " + MAX_PASSWORD_LENGTH + " characters."));
        } else if (currentOk && next.equals(current)) {
            violations.add(new FieldViolation("newPassword",
                    "New password must be different from the current password."));
        }
        if (next != null && !next.equals(request.confirmPassword())) {
            violations.add(new FieldViolation("confirmPassword", "Passwords don't match."));
        }
        if (!violations.isEmpty()) {
            throw new PasswordChangeRefusedException(violations);
        }

        user.resetPassword(next);
        user.setMustChangePassword(false);
        return userRepository.merge(user);
    }

    /** The change was refused; the transaction rolls back. */
    public static class PasswordChangeRefusedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final List<FieldViolation> violations;

        public PasswordChangeRefusedException(List<FieldViolation> violations) {
            super("Password not changed.");
            this.violations = List.copyOf(violations);
        }

        public List<FieldViolation> getViolations() {
            return violations;
        }
    }
}
