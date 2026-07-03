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
package com.rreganjr.requel.gateway.rest;

import java.time.Instant;

/**
 * The tokens from an OAuth token response, as the {@code requel} CLI stores and uses them. The
 * refresh token may be {@code null} (e.g. a response that didn't rotate one, though Requel's AS
 * always issues one). {@code expiresAt} is computed from {@code expires_in} at fetch time.
 *
 * @param accessToken  the bearer access token
 * @param refreshToken the rotating refresh token, or {@code null}
 * @param scope        the granted scope (space-delimited), or {@code null}
 * @param expiresAt    the access token's expiry instant
 */
public record OAuthTokens(String accessToken, String refreshToken, String scope, Instant expiresAt) {

    /**
     * @return {@code true} if the access token is expired (or within {@code skew} of expiry) at
     *         {@code now}, so the caller should refresh before using it. A {@code null}
     *         {@code expiresAt} is treated as expired.
     */
    public boolean isExpired(Instant now, java.time.Duration skew) {
        return expiresAt == null || !now.plus(skew).isBefore(expiresAt);
    }
}
