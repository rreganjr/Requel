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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Authentication filter for the bearer token in the Authorization header. Branches on credential
 * type and sets the Spring Security context. Placed before UsernamePasswordAuthenticationFilter.
 *
 * <p>Two credential kinds (issue #73):
 * <ul>
 *   <li><b>Login JWT</b> — short-lived, self-contained; validated statelessly (signature/expiry)
 *       with authorities from its claims. No DB hit.</li>
 *   <li><b>Personal access token (PAT)</b> — opaque, prefixed {@code reqpat_}; hashed and looked up
 *       in the token store on <em>every</em> request (so revocation/expiry take effect immediately),
 *       the owning user is resolved, and authorities are loaded <em>live</em> from that user (so
 *       role changes take effect immediately). {@code last_used_at} is updated, throttled.</li>
 * </ul>
 * A PAT only establishes the triggering user; command authorization (stakeholder permissions) and
 * audit are unchanged.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    /** Update last_used_at at most once per this many seconds, to avoid a write per request. */
    private static final long LAST_USED_THROTTLE_SECONDS = 60L;

    private final JwtService jwtService;
    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ApiTokenRepository apiTokenRepository,
            UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.jwtService = jwtService;
        this.apiTokenRepository = apiTokenRepository;
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (ApiTokenService.isApiToken(token)) {
                authenticateApiToken(token);
            } else {
                authenticateJwt(token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateJwt(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList()
                    : List.of();

            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            // Store JWT expiry epoch-ms as details so controllers can schedule server-side expiry
            Date exp = claims.getExpiration();
            auth.setDetails(exp != null ? exp.getTime() : 0L);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            // Invalid or expired token — don't set authentication, let Spring Security
            // return 401 for protected endpoints
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Validate a personal access token: hash, look up, check active (not revoked / not expired),
     * resolve the owning user, and set the security context with authorities loaded live from the
     * user. Any failure leaves the context unauthenticated so protected endpoints return 401.
     */
    private void authenticateApiToken(String token) {
        try {
            Optional<ApiToken> found = apiTokenRepository.findByTokenHash(ApiTokenService.hash(token));
            Instant now = Instant.now();
            if (found.isEmpty() || !found.get().isActive(now)) {
                SecurityContextHolder.clearContext();
                return;
            }
            ApiToken pat = found.get();
            User user = userRepository.findUserById(pat.getOwnerUserId());
            if (user == null) {
                SecurityContextHolder.clearContext();
                return;
            }
            List<SimpleGrantedAuthority> authorities = userDtoMapper.getRoleStrings(user).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
            auth.setDetails(pat.getExpiresAt() != null ? pat.getExpiresAt().toEpochMilli() : 0L);
            SecurityContextHolder.getContext().setAuthentication(auth);
            touchLastUsed(pat, now);
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
        }
    }

    /** Best-effort, throttled last-used bookkeeping; never fails the request. */
    private void touchLastUsed(ApiToken pat, Instant now) {
        Instant last = pat.getLastUsedAt();
        if (last == null || last.isBefore(now.minusSeconds(LAST_USED_THROTTLE_SECONDS))) {
            try {
                pat.setLastUsedAt(now);
                apiTokenRepository.save(pat);
            } catch (RuntimeException ignored) {
                // last-used is non-critical; don't break auth over a bookkeeping write
            }
        }
    }
}
