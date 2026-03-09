package com.rreganjr.requel.service.auth;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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

    public JwtService(
            @Value("${requel.jwt.secret:requel-dev-secret-change-in-production-min-32-chars!!}") String secret,
            @Value("${requel.jwt.expiry-hours:8}") int expiryHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryHours * 3600L * 1000L;
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
