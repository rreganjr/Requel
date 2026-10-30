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

import java.io.IOException;
import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Issue #381: a caller who must change their password can do that and nothing else.
 *
 * <p>Every authentication path gives such a caller the single authority
 * {@link #AUTHORITY} in place of the user's roles: a login JWT carrying
 * {@link JwtService#PASSWORD_CHANGE_CLAIM}, and a personal access token or MCP OAuth token whose
 * owner has the flag (those two load the user live, see {@link JwtAuthenticationFilter} and
 * {@link McpJwtAuthenticationConverter}). This filter, in both the {@code /api/**} chain and the
 * MCP/gateway chain, answers 403 {@code PASSWORD_CHANGE_REQUIRED} to that caller on every request
 * except the few that let them sign in, see who they are and change the password.</p>
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    /** The authority, and the error code in the 403 body. */
    public static final String AUTHORITY = "PASSWORD_CHANGE_REQUIRED";

    /** Granted to a caller who must change their password, instead of their roles. */
    public static final SimpleGrantedAuthority GRANTED_AUTHORITY = new SimpleGrantedAuthority(AUTHORITY);

    static final String MESSAGE = "You must change your password before doing anything else.";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && isRestricted(auth.getAuthorities()) && !isAllowed(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + AUTHORITY + "\",\"message\":\"" + MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean isRestricted(Collection<? extends GrantedAuthority> authorities) {
        return authorities != null && authorities.stream()
                .anyMatch(a -> AUTHORITY.equals(a.getAuthority()));
    }

    /**
     * Login stays open (a stale stored token may ride along on it), {@code /me} tells the UI who
     * is signed in, and the change itself.
     */
    private static boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        return ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/auth/password".equals(path)))
                || ("GET".equals(method) && "/api/auth/me".equals(path))
                || "OPTIONS".equals(method);
    }
}
