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

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.gateway.rest.AsMetadata;
import com.rreganjr.requel.gateway.rest.OAuthClient;
import com.rreganjr.requel.gateway.rest.OAuthTokens;
import java.time.Instant;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class CliTokenSourceTest {

    private static final String URL = "http://localhost:8080";

    private static Function<String, OAuthClient> noRefresh() {
        return url -> {
            throw new AssertionError("refresh must not be attempted");
        };
    }

    @Test
    void explicitTokenWinsOverEverything(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.save(URL, "reqpat_STORED");
        store.saveOAuth(URL, new OAuthTokens("AT", "RT", "mcp", Instant.now().plusSeconds(3600)));

        CliTokenSource src = new CliTokenSource("reqpat_FLAG", URL, store, noRefresh());
        assertThat(src.currentToken()).isEqualTo("reqpat_FLAG");
    }

    @Test
    void returnsValidOAuthAccessTokenWithoutRefreshing(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.saveOAuth(URL, new OAuthTokens("AT", "RT", "mcp", Instant.now().plusSeconds(3600)));

        CliTokenSource src = new CliTokenSource(null, URL, store, noRefresh());
        assertThat(src.currentToken()).isEqualTo("AT");
    }

    @Test
    void refreshesExpiredOAuthTokenAndPersistsTheRotation(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.saveOAuth(URL, new OAuthTokens("OLD_AT", "OLD_RT", "mcp", Instant.now().minusSeconds(10)));

        OAuthClient fake = new OAuthClient(URL) {
            @Override
            public AsMetadata discover() {
                return new AsMetadata("iss", "auth", URL + "/oauth2/token");
            }

            @Override
            public OAuthTokens refresh(String tokenEndpoint, String clientId, String refreshToken) {
                assertThat(refreshToken).isEqualTo("OLD_RT");
                assertThat(clientId).isEqualTo(CliTokenSource.CLI_CLIENT_ID);
                return new OAuthTokens("NEW_AT", "NEW_RT", "mcp", Instant.now().plusSeconds(3600));
            }
        };

        CliTokenSource src = new CliTokenSource(null, URL, store, url -> fake);
        assertThat(src.currentToken()).isEqualTo("NEW_AT");
        assertThat(store.findOAuth(URL).accessToken()).isEqualTo("NEW_AT");
        assertThat(store.findOAuth(URL).refreshToken()).isEqualTo("NEW_RT");
    }

    @Test
    void returnsStaleAccessTokenWhenRefreshFails(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.saveOAuth(URL, new OAuthTokens("STALE_AT", "RT", "mcp", Instant.now().minusSeconds(10)));

        OAuthClient failing = new OAuthClient(URL) {
            @Override
            public AsMetadata discover() {
                throw new RuntimeException("offline");
            }
        };

        CliTokenSource src = new CliTokenSource(null, URL, store, url -> failing);
        assertThat(src.currentToken()).isEqualTo("STALE_AT");
    }

    @Test
    void fallsBackToPatWhenNoOAuth(@TempDir Path dir) {
        CredentialStore store = new CredentialStore(dir);
        store.save(URL, "reqpat_STORED");

        CliTokenSource src = new CliTokenSource(null, URL, store, noRefresh());
        assertThat(src.currentToken()).isEqualTo("reqpat_STORED");
    }

    @Test
    void returnsNullWhenNothingConfigured(@TempDir Path dir) {
        CliTokenSource src = new CliTokenSource(null, URL, new CredentialStore(dir), noRefresh());
        assertThat(src.currentToken()).isNull();
    }
}
