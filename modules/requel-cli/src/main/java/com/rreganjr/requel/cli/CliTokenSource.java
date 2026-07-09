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
package com.rreganjr.requel.cli;

import com.rreganjr.requel.gateway.rest.AsMetadata;
import com.rreganjr.requel.gateway.rest.BearerTokenSource;
import com.rreganjr.requel.gateway.rest.OAuthClient;
import com.rreganjr.requel.gateway.rest.OAuthTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Resolves the bearer token for each REST call, in precedence order:
 * <ol>
 *   <li>an explicit token ({@code --token} flag / {@code REQUEL_TOKEN} env);</li>
 *   <li>OAuth tokens stored by {@code requel login} — the access token, transparently refreshed via
 *       the rotating refresh token when it is at/near expiry (the store is updated with the rotated
 *       tokens);</li>
 *   <li>a stored PAT (#73) for the URL.</li>
 * </ol>
 * Returns {@code null} when nothing is configured (no Authorization header is sent). If a refresh
 * fails, the (stale) access token is returned so the server — not the client — decides; the user then
 * re-runs {@code requel login}.
 */
public class CliTokenSource implements BearerTokenSource {

    /** The first-party client id; must match the seeded {@code requel-cli} AS client. */
    public static final String CLI_CLIENT_ID = "requel-cli";

    /** Refresh when the access token is within this window of expiry. */
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    private final String explicitToken;
    private final String url;
    private final CredentialStore store;
    private final Function<String, OAuthClient> oauthClientFactory;

    public CliTokenSource(String explicitToken, String url, CredentialStore store) {
        this(explicitToken, url, store, OAuthClient::new);
    }

    /** For tests: inject an {@link OAuthClient} factory (keyed by base URL). */
    CliTokenSource(String explicitToken, String url, CredentialStore store,
            Function<String, OAuthClient> oauthClientFactory) {
        this.explicitToken = explicitToken;
        this.url = url;
        this.store = store;
        this.oauthClientFactory = oauthClientFactory;
    }

    @Override
    public String currentToken() {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return explicitToken;
        }
        OAuthTokens tokens = store.findOAuth(url);
        if (tokens != null) {
            return tokens.isExpired(Instant.now(), EXPIRY_SKEW) ? refreshed(tokens) : tokens.accessToken();
        }
        return store.find(url);
    }

    private String refreshed(OAuthTokens stale) {
        if (stale.refreshToken() == null || stale.refreshToken().isBlank()) {
            return stale.accessToken();
        }
        try {
            OAuthClient client = oauthClientFactory.apply(url);
            AsMetadata meta = client.discover();
            OAuthTokens fresh = client.refresh(meta.tokenEndpoint(), CLI_CLIENT_ID, stale.refreshToken());
            store.saveOAuth(url, fresh);
            return fresh.accessToken();
        } catch (RuntimeException e) {
            // Refresh failed (expired/revoked/offline): return the stale token and let the server
            // reject it, prompting the user to re-run `requel login`.
            return stale.accessToken();
        }
    }
}
