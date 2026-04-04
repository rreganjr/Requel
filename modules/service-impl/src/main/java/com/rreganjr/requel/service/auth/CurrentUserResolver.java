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

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves the current authenticated user from the Spring Security context
 * to a domain User entity. Used by CommandController to set editedBy on commands
 * and by query controllers for access control.
 */
@Service
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolve the currently authenticated principal to a domain User.
     *
     * @return the domain User for the authenticated principal
     * @throws com.rreganjr.requel.user.exception.NoSuchUserException if user not found
     * @throws IllegalStateException if no authentication is present
     */
    public User resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        String username = auth.getName();
        return userRepository.findUserByUsername(username);
    }

    /**
     * Resolve a specific username to a domain User.
     */
    public User resolve(String username) {
        return userRepository.findUserByUsername(username);
    }
}
