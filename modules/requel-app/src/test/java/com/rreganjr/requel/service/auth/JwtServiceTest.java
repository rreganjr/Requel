/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.platform.identity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtService}.
 *
 * No Spring context — {@code JwtService} is constructed directly with a known
 * secret and expiry, so tests are fast and deterministic.
 *
 * Scenarios covered:
 * - generateToken: subject, roles, and permissions claims are present
 * - generateToken: expiry claim is set in the future
 * - generateToken: issuedAt claim is set
 * - getExpiryMs: returns the configured value
 * - parseToken: round-trip — parse recovers all claims from a freshly generated token
 * - getUsername: extracts subject from token
 * - parseToken: rejects a token signed with a different key
 * - parseToken: rejects a structurally malformed token string
 * - parseToken: rejects an expired token
 */
class JwtServiceTest {

    // Minimum 32 ASCII chars for HS256
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256!!";
    private static final int EXPIRY_HOURS = 8;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRY_HOURS);

        user = mock(User.class);
        when(user.getUsername()).thenReturn("alice");
    }

    // -------------------------------------------------------------------------
    // generateToken — claims
    // -------------------------------------------------------------------------

    @Test
    void generateTokenSubjectIsUsername() {
        String token = jwtService.generateToken(user, List.of(), List.of());

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void generateTokenRolesClaimIsPresent() {
        String token = jwtService.generateToken(user,
                List.of("ProjectUserRole", "SystemAdminUserRole"), List.of());

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.get("roles", List.class))
                .containsExactly("ProjectUserRole", "SystemAdminUserRole");
    }

    @Test
    void generateTokenPermissionsClaimIsPresent() {
        String token = jwtService.generateToken(user,
                List.of(), List.of("createProject", "addStakeholder"));

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.get("permissions", List.class))
                .containsExactly("createProject", "addStakeholder");
    }

    @Test
    void generateTokenExpiryIsInTheFuture() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(user, List.of(), List.of());
        long after = System.currentTimeMillis();

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getExpiration().getTime())
                .isGreaterThan(before)
                .isGreaterThanOrEqualTo(before + EXPIRY_HOURS * 3600L * 1000L - 1000L)
                .isLessThanOrEqualTo(after  + EXPIRY_HOURS * 3600L * 1000L + 1000L);
    }

    @Test
    void generateTokenIssuedAtIsSet() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(user, List.of(), List.of());
        long after = System.currentTimeMillis();

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getIssuedAt().getTime())
                .isGreaterThanOrEqualTo(before - 1000L)
                .isLessThanOrEqualTo(after  + 1000L);
    }

    // -------------------------------------------------------------------------
    // getExpiryMs
    // -------------------------------------------------------------------------

    @Test
    void getExpiryMsReturnsConfiguredValue() {
        assertThat(jwtService.getExpiryMs()).isEqualTo(EXPIRY_HOURS * 3600L * 1000L);
    }

    // -------------------------------------------------------------------------
    // getUsername
    // -------------------------------------------------------------------------

    @Test
    void getUsernameExtractsSubject() {
        String token = jwtService.generateToken(user, List.of(), List.of());
        assertThat(jwtService.getUsername(token)).isEqualTo("alice");
    }

    // -------------------------------------------------------------------------
    // parseToken — rejection cases
    // -------------------------------------------------------------------------

    @Test
    void parseTokenRejectsTokenSignedWithDifferentKey() {
        JwtService other = new JwtService("a-completely-different-secret-key-also-long!!", EXPIRY_HOURS);
        String foreignToken = other.generateToken(user, List.of(), List.of());

        assertThatThrownBy(() -> jwtService.parseToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseTokenRejectsMalformedTokenString() {
        assertThatThrownBy(() -> jwtService.parseToken("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseTokenRejectsExpiredToken() {
        // Construct a service with -1 hour expiry — token is expired the moment it's created
        JwtService expiredService = new JwtService(SECRET, -1);
        String expiredToken = expiredService.generateToken(user, List.of(), List.of());

        assertThatThrownBy(() -> jwtService.parseToken(expiredToken))
                .isInstanceOf(JwtException.class);
    }
}
