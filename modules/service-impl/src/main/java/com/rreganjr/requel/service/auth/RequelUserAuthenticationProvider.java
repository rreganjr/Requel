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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the OAuth 2.1 authorization server's interactive login form against the Requel user
 * store (issue #83, Slice 1). The embedded authorization server issues tokens for a Requel user, so
 * the human who logs in during the authorization-code flow must present real Requel credentials.
 *
 * <p>This is the OAuth login-page counterpart to {@link AuthController#login}: it reuses the same
 * domain credential check ({@link User#isPassword(String)}) and the same role mapping
 * ({@link UserDtoMapper#getRoleStrings(User)} → {@code ROLE_<role>}) as the SPA's JWT login and the
 * PAT path, so all three authentication surfaces resolve identical principals and authorities.
 *
 * <p>Registered as a single {@link AuthenticationProvider} bean; Spring Security's
 * auto-configuration wires it into the global {@code AuthenticationManager} used by the form-login
 * filter chain. It does not affect the stateless JWT chain on {@code /api/**}, which never invokes
 * {@code authenticate()}.
 */
@Component
public class RequelUserAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    public RequelUserAuthenticationProvider(UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null
                ? null : authentication.getCredentials().toString();

        User user;
        try {
            user = userRepository.findUserByUsername(username);
        } catch (RuntimeException e) {
            // NoSuchUserException (or any lookup failure) — do not disclose which; treat as bad creds.
            throw new BadCredentialsException("Invalid username or password");
        }
        if (user == null || password == null || !user.isPassword(password)) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<SimpleGrantedAuthority> authorities = userDtoMapper.getRoleStrings(user).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        // Credentials are erased; principal name is the Requel username so CurrentUserResolver and
        // the token 'sub' resolve back to this user.
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
