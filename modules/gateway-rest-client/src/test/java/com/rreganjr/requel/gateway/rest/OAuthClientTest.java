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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OAuthClientTest {

    private record Fixture(OAuthClient client, MockRestServiceServer server) {
    }

    private static Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new OAuthClient(builder.build()), server);
    }

    @Test
    void discoverParsesEndpoints() {
        Fixture f = newFixture();
        f.server().expect(requestTo("/.well-known/oauth-authorization-server"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"issuer":"http://localhost:8080",
                         "authorization_endpoint":"http://localhost:8080/oauth2/authorize",
                         "token_endpoint":"http://localhost:8080/oauth2/token"}
                        """, MediaType.APPLICATION_JSON));

        AsMetadata meta = f.client().discover();

        assertThat(meta.authorizationEndpoint()).isEqualTo("http://localhost:8080/oauth2/authorize");
        assertThat(meta.tokenEndpoint()).isEqualTo("http://localhost:8080/oauth2/token");
        f.server().verify();
    }

    @Test
    void exchangeAuthorizationCodePostsFormAndComputesExpiry() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://localhost:8080/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "grant_type=authorization_code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code_verifier=verifier")))
                .andRespond(withSuccess("""
                        {"access_token":"AT","refresh_token":"RT","scope":"mcp",
                         "token_type":"Bearer","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

        Instant before = Instant.now();
        OAuthTokens tokens = f.client().exchangeAuthorizationCode(
                "http://localhost:8080/oauth2/token", "requel-cli",
                "http://127.0.0.1:5555/callback", "the-code", "verifier");

        assertThat(tokens.accessToken()).isEqualTo("AT");
        assertThat(tokens.refreshToken()).isEqualTo("RT");
        assertThat(tokens.scope()).isEqualTo("mcp");
        assertThat(tokens.expiresAt()).isAfter(before.plus(Duration.ofSeconds(3000)));
        assertThat(tokens.isExpired(Instant.now(), Duration.ofSeconds(60))).isFalse();
        f.server().verify();
    }

    @Test
    void refreshPostsRefreshGrant() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://localhost:8080/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("refresh_token=OLD")))
                .andRespond(withSuccess("""
                        {"access_token":"AT2","refresh_token":"NEW","scope":"mcp","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

        OAuthTokens tokens = f.client().refresh(
                "http://localhost:8080/oauth2/token", "requel-cli", "OLD");

        assertThat(tokens.accessToken()).isEqualTo("AT2");
        assertThat(tokens.refreshToken()).isEqualTo("NEW");
        f.server().verify();
    }
}
