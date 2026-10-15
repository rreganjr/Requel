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
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Turns an authorization-server-issued access token into an authentication for the MCP resource
 * server (issue #83, Slice 2), mapping the token subject to a Requel user and loading that user's
 * authorities <em>live</em> from the user store rather than trusting role claims in the token.
 *
 * <p>The token {@code sub} is the Requel username (set at issuance), so the resulting
 * {@link JwtAuthenticationToken}'s name matches what {@link CurrentUserResolver} expects — the
 * gateway then runs per-stakeholder authorization as that user, exactly as the JWT and PAT paths do.
 * Authorities combine the user's roles ({@code ROLE_<role>}, via {@link UserDtoMapper}) with the
 * token's scopes ({@code SCOPE_<scope>}), so both role- and scope-based checks are available.
 *
 * <p>If the subject does not resolve to a user, the token is rejected
 * ({@link InvalidBearerTokenException}) rather than authenticating an unknown principal.
 */
public class McpJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;
    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    public McpJwtAuthenticationConverter(UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        User user;
        try {
            user = (username != null) ? userRepository.findUserByUsername(username) : null;
        } catch (RuntimeException e) {
            throw new InvalidBearerTokenException("Token subject does not map to a Requel user");
        }
        if (user == null) {
            throw new InvalidBearerTokenException("Token subject does not map to a Requel user");
        }

        Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String role : userDtoMapper.getRoleStrings(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }

        return new JwtAuthenticationToken(jwt, authorities, user.getUsername());
    }
}
