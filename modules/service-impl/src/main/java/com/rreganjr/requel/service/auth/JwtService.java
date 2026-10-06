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

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * JWT token generation and validation using jjwt (HS256).
 * Claims: sub (username), roles, permissions, exp.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryMs;

    /** The secret {@code application.properties} falls back to when REQUEL_JWT_SECRET is unset. */
    public static final String BUILT_IN_SECRET = "requel-dev-secret-change-in-production-min-32-chars";

    /** HS256 needs a key of at least 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    /**
     * Issue #376: outside the {@code dev} and {@code test} profiles, refuse to start on the
     * built-in secret (anyone could sign a token with it) or on a blank one; in any profile,
     * refuse a secret too short for HS256 with a message instead of a {@code WeakKeyException}.
     */
    @Autowired
    public JwtService(
            @Value("${requel.jwt.secret}") String secret,
            @Value("${requel.jwt.expiry-hours:8}") int expiryHours,
            Environment environment) {
        this(checkedSecret(secret, environment.getActiveProfiles()), expiryHours);
    }

    /** For tests: no profile check. */
    public JwtService(String secret, int expiryHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryHours * 3600L * 1000L;
    }

    static String checkedSecret(String secret, String[] activeProfiles) {
        boolean devOrTest = Arrays.stream(activeProfiles).anyMatch(p -> p.equals("dev") || p.equals("test"));
        if (secret == null || secret.isBlank() || secret.equals(BUILT_IN_SECRET)) {
            if (!devOrTest) {
                throw new InsecureJwtSecretException("REQUEL_JWT_SECRET is not set, so tokens would be"
                        + " signed with Requel's built-in secret, which anyone can read.");
            }
            return BUILT_IN_SECRET;
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new InsecureJwtSecretException("REQUEL_JWT_SECRET is " + secret.getBytes(StandardCharsets.UTF_8).length
                    + " bytes; it needs at least " + MIN_SECRET_BYTES + ".");
        }
        return secret;
    }

    /**
     * Generate a JWT for the given user with roles and permissions as claims.
     */
    public String generateToken(User user, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate and parse the JWT, returning claims if valid.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the username (subject) from a token.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * @return the expiry duration in milliseconds (for SSE session expiry scheduling)
     */
    public long getExpiryMs() {
        return expiryMs;
    }
}
