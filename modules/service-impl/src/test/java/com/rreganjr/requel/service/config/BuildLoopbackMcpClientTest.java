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

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for the shared DCR client policy ({@link AuthorizationServerConfig#buildLoopbackMcpClient})
 * that both the gated OIDC converter and the loopback-anonymous filter use (issue #238). Pure — no AS
 * boot required.
 */
class BuildLoopbackMcpClientTest {

    @Test
    void stampsPublicPkceConsentScopeMcpOnLoopbackClient() {
        RegisteredClient client = AuthorizationServerConfig.buildLoopbackMcpClient(
                "codex", List.of("http://127.0.0.1:8899/callback", "http://localhost:8899/callback"));

        assertTrue(client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        assertTrue(client.getClientSettings().isRequireProofKey());
        assertTrue(client.getClientSettings().isRequireAuthorizationConsent());
        assertEquals(1, client.getScopes().size());
        assertTrue(client.getScopes().contains("mcp"));
        assertTrue(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE));
        assertTrue(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
        assertEquals(2, client.getRedirectUris().size());
    }

    @Test
    void rejectsNonLoopbackRedirectUri() {
        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class, () ->
                AuthorizationServerConfig.buildLoopbackMcpClient(
                        "bad", List.of("https://evil.example.com/cb")));
        assertEquals("invalid_redirect_uri", ex.getError().getErrorCode());
    }

    @Test
    void rejectsMixedLoopbackAndNonLoopback() {
        assertThrows(OAuth2AuthenticationException.class, () ->
                AuthorizationServerConfig.buildLoopbackMcpClient(
                        "mixed", List.of("http://127.0.0.1:9000/cb", "https://evil.example.com/cb")));
    }

    @Test
    void rejectsEmptyRedirectUris() {
        assertThrows(OAuth2AuthenticationException.class, () ->
                AuthorizationServerConfig.buildLoopbackMcpClient("empty", List.of()));
    }
}
