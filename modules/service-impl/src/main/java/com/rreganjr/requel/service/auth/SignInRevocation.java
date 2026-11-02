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

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.user.User;

/**
 * Ends a user's other sign-ins after a password change (issue #446).
 *
 * <ul>
 * <li>Login tokens and OAuth access tokens are refused once they predate
 * {@link User#getPasswordChangedAt()}: {@link JwtAuthenticationFilter} and
 * {@link McpJwtAuthenticationConverter} check it on every request, so nothing is stored for
 * them.</li>
 * <li>The user's OAuth authorizations are deleted here, so their refresh tokens stop working.</li>
 * <li>Personal access tokens are long-lived tokens the user manages: their own change keeps them;
 * an admin reset revokes them all, since a reset usually means the account was compromised.</li>
 * </ul>
 */
@Component
public class SignInRevocation {

    private static final Logger log = LoggerFactory.getLogger(SignInRevocation.class);

    private final JdbcTemplate jdbcTemplate;
    private final ApiTokenRepository apiTokenRepository;

    public SignInRevocation(JdbcTemplate jdbcTemplate, ApiTokenRepository apiTokenRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.apiTokenRepository = apiTokenRepository;
    }

    /**
     * @param revokeApiTokens
     *            true for an admin reset: also revoke every personal access token the user has
     */
    @Transactional
    public void afterPasswordChange(User user, boolean revokeApiTokens) {
        int authorizations = 0;
        try {
            authorizations = jdbcTemplate.update(
                    "DELETE FROM oauth2_authorization WHERE principal_name = ?", user.getUsername());
        } catch (DataAccessException e) {
            // Caught here, inside the transaction, so a failure can't undo the password change.
            // Flyway creates the table (V12); only a test context without it lands here.
            log.warn("Could not end OAuth authorizations for user '{}': {}", user.getUsername(),
                    e.getMessage());
        }
        int tokens = 0;
        if (revokeApiTokens) {
            for (ApiToken token : apiTokenRepository.findByOwnerUserIdOrderByCreatedAtDesc(user.getId())) {
                if (!token.isRevoked()) {
                    token.setRevoked(true);
                    apiTokenRepository.save(token);
                    tokens++;
                }
            }
        }
        log.info("Password changed for user '{}': ended {} OAuth authorization(s), revoked {} access"
                + " token(s); sign-ins from before {} are refused", user.getUsername(), authorizations,
                tokens, user.getPasswordChangedAt());
    }

    /**
     * Whether a token issued at {@code issuedAt} predates the user's last password change. Both are
     * to the second, so the token a change returns (issued in the same second) is still good.
     */
    public static boolean predatesPasswordChange(User user, Instant issuedAt) {
        Instant changedAt = user.getPasswordChangedAt();
        return changedAt != null && issuedAt != null
                && issuedAt.getEpochSecond() < changedAt.getEpochSecond();
    }
}
