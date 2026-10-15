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
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoginOAuthTest {

    private static final String URL = "http://localhost:8080";

    /** A fake AS: discovery returns endpoints; the code exchange returns fixed tokens. */
    private static OAuthClient fakeAs() {
        return new OAuthClient(URL) {
            @Override
            public AsMetadata discover() {
                return new AsMetadata("iss", URL + "/oauth2/authorize", URL + "/oauth2/token");
            }

            @Override
            public OAuthTokens exchangeAuthorizationCode(String tokenEndpoint, String clientId,
                    String redirectUri, String code, String codeVerifier) {
                assertThat(clientId).isEqualTo(CliTokenSource.CLI_CLIENT_ID);
                assertThat(code).isEqualTo("test-code");
                assertThat(codeVerifier).isNotBlank();
                return new OAuthTokens("AT", "RT", "mcp", Instant.now().plusSeconds(3600));
            }
        };
    }

    /** Simulates the user's browser: parse the authorize URL and hit the loopback callback. */
    private static void simulateBrowser(String authorizeUrl) {
        Map<String, String> q = query(authorizeUrl);
        String callback = q.get("redirect_uri") + "?code=test-code&state="
                + URLDecoder.decode(q.get("state"), StandardCharsets.UTF_8);
        try {
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(callback)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void oauthLoginExchangesCodeAndStoresTokens(@TempDir Path dir) {
        RequelCli parent = new RequelCli();
        parent.url = URL;
        parent.credentialStore = new CredentialStore(dir);

        LoginCommand login = new LoginCommand();
        login.parent = parent;
        login.mode.oauth = true;
        login.oauthClientOverride = fakeAs();
        login.browserOpener = LoginOAuthTest::simulateBrowser;

        assertThat(login.call()).isEqualTo(ExitCode.SUCCESS);

        OAuthTokens stored = parent.credentialStore.findOAuth(URL);
        assertThat(stored).isNotNull();
        assertThat(stored.accessToken()).isEqualTo("AT");
        assertThat(stored.refreshToken()).isEqualTo("RT");
    }

    private static Map<String, String> query(String url) {
        Map<String, String> params = new HashMap<>();
        String q = URI.create(url).getRawQuery();
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            params.put(pair.substring(0, eq),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }
}
