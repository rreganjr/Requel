package com.rreganjr.requel.service.auth;

import com.rreganjr.platform.identity.User;
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
