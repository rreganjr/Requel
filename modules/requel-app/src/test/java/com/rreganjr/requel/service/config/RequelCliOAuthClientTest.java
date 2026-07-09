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
package com.rreganjr.requel.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * Unit test for the seeded {@code requel-cli} OAuth client (#70, Milestone 5): asserts the security
 * shape the CLI's {@code requel login} depends on — public/PKCE, loopback redirect, consent, the
 * {@code mcp} scope, and authorization-code + refresh with the standard 1h/30d token settings.
 */
class RequelCliOAuthClientTest {

    private final RegisteredClient client = AuthorizationServerConfig.requelCliRegisteredClient();

    @Test
    void isPublicPkceLoopbackClientScopedToMcp() {
        assertThat(client.getClientId()).isEqualTo(AuthorizationServerConfig.CLI_CLIENT_ID);
        assertThat(client.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(client.getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(client.getScopes()).containsExactly("mcp");
        assertThat(client.getRedirectUris()).containsExactly(AuthorizationServerConfig.CLI_REDIRECT_URI);
        assertThat(AuthorizationServerConfig.CLI_REDIRECT_URI).startsWith("http://127.0.0.1");
    }

    @Test
    void requiresPkceAndConsent() {
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getClientSettings().isRequireAuthorizationConsent()).isTrue();
    }

    @Test
    void usesTheStandardRotatingTokenSettings() {
        assertThat(client.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofHours(1));
        assertThat(client.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofDays(30));
        assertThat(client.getTokenSettings().isReuseRefreshTokens()).isFalse();
    }
}
