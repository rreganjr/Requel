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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration coverage for loopback-restricted anonymous Dynamic Client Registration (issue #238),
 * with the feature enabled ({@code requel.oauth.dcr.allow-anonymous-loopback=true}).
 *
 * <p>Drives the real {@code POST /connect/register} endpoint through the full security filter chain
 * (MockMvc's default remote address is {@code 127.0.0.1}, so requests count as loopback peers). The
 * gated-only regression (property off) lives in {@link AnonymousLoopbackDcrDisabledIT}.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = { "requel.oauth.dcr.allow-anonymous-loopback=true" })
@Sql(scripts = "/db/oauth2-schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class AnonymousLoopbackDcrIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void anonymousLoopbackRegistrationSucceedsAndIsPolicyStamped() throws Exception {
        String body = "{\"client_name\":\"codex\","
                + "\"redirect_uris\":[\"http://127.0.0.1:8899/callback\",\"http://localhost:8899/callback\"],"
                + "\"grant_types\":[\"authorization_code\",\"refresh_token\"]}";

        MvcResult result = mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").exists())
                .andExpect(jsonPath("$.scope").value("mcp"))
                .andExpect(jsonPath("$.token_endpoint_auth_method").value("none"))
                .andReturn();

        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("client_id").asText();

        // The registered client carries the same policy as gated DCR: public/PKCE, consent, scope mcp.
        RegisteredClient saved = registeredClientRepository.findByClientId(clientId);
        assertNotNull(saved, "anonymously-registered client should be persisted");
        assertTrue(saved.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        assertTrue(saved.getClientSettings().isRequireProofKey());
        assertTrue(saved.getClientSettings().isRequireAuthorizationConsent());
        assertEquals(1, saved.getScopes().size());
        assertTrue(saved.getScopes().contains("mcp"));
        assertEquals(2, saved.getRedirectUris().size());
    }

    @Test
    void authorizationServerMetadataAdvertisesRegistrationEndpoint() throws Exception {
        // MCP clients (Codex) read the RFC 8414 doc to decide DCR support; it must carry
        // registration_endpoint when the loopback-anonymous path is enabled (issue #238).
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration_endpoint").exists());
    }

    @Test
    void nonLoopbackRedirectUriIsRejected() throws Exception {
        String body = "{\"client_name\":\"bad\",\"redirect_uris\":[\"https://evil.example.com/cb\"]}";

        mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_redirect_uri"));
    }

    @Test
    void nonLoopbackPeerFallsThroughToGatedFlow() throws Exception {
        // Same (loopback) redirect URIs, but the request arrives from a non-loopback peer: the filter
        // must NOT handle it; it falls through to Spring AS's gated endpoint, which redirects the
        // unauthenticated request to the AS login page (302).
        String body = "{\"client_name\":\"remote\",\"redirect_uris\":[\"http://127.0.0.1:8899/callback\"]}";

        mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(request -> { request.setRemoteAddr("203.0.113.7"); return request; }))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authorizeHopRejectsUnregisteredRedirectUri() throws Exception {
        // Register a loopback client, then start an /oauth2/authorize with a redirect_uri that was NOT
        // registered (different path — loopback port relaxation does not relax the path). Spring AS
        // validates redirect_uri at the authorize endpoint, so this is rejected (no redirect to it).
        // Guards the point raised in issue #238 review: the loopback allowance is re-checked on the
        // authorize hop, not only at registration.
        String reg = "{\"client_name\":\"authz\","
                + "\"redirect_uris\":[\"http://127.0.0.1:8899/callback\"],"
                + "\"grant_types\":[\"authorization_code\"]}";
        MvcResult result = mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated())
                .andReturn();
        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("client_id").asText();

        mockMvc.perform(get("/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", clientId)
                        .param("redirect_uri", "http://127.0.0.1:8899/not-registered")
                        .param("scope", "mcp")
                        .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                        .param("code_challenge_method", "S256")
                        .param("state", "xyz"))
                .andExpect(status().is4xxClientError());
    }
}
