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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Issue #394: every Requel OAuth client is public (PKCE, no secret) — {@code requel-cli} and the
 * MCP clients that register themselves. They must get a refresh token at login and be able to
 * redeem it, or every OAuth session ends when the 1-hour access token does. Runs the real flow
 * against the authorization server: authorize, consent, code + PKCE exchange, refresh, and the
 * rotation / client-binding checks.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = { "requel.oauth.dcr.allow-anonymous-loopback=true" })
@Sql(scripts = "/db/oauth2-schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class PublicClientRefreshTokenIT extends AbstractIntegrationTestCase {

    /** RFC 7636 appendix B. */
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String REDIRECT = "http://127.0.0.1:53111/callback";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void theCliGetsARefreshTokenAndCanRedeemIt() throws Exception {
        String clientId = cliClientId();
        JsonNode tokens = login(clientId);
        assertNotNull(tokens.get("refresh_token"), "the public CLI client must get a refresh token");
        assertRotates(clientId, tokens.get("refresh_token").asText());
    }

    @Test
    void aSelfRegisteredMcpClientGetsARefreshTokenAndCanRedeemIt() throws Exception {
        String clientId = registerLoopbackClient("claude-code");
        JsonNode tokens = login(clientId);
        assertNotNull(tokens.get("refresh_token"), "a DCR-registered public client must get a refresh token");
        assertRotates(clientId, tokens.get("refresh_token").asText());
    }

    @Test
    void aRefreshTokenOnlyWorksForTheClientItWasIssuedTo() throws Exception {
        String owner = registerLoopbackClient("owner");
        String other = registerLoopbackClient("other");
        String refresh = refreshTokenFor(owner);

        refresh(other, refresh)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void aRefreshRequestWithoutAClientIdIsRefused() throws Exception {
        String refresh = refreshTokenFor(registerLoopbackClient("no-client-id"));

        // Nothing authenticates the client, so the endpoint refuses it (a browser would be sent to
        // the login page instead; OAuth clients ask for JSON).
        mockMvc.perform(post("/oauth2/token")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anUnknownClientIdIsRefused() throws Exception {
        String refresh = refreshTokenFor(registerLoopbackClient("unknown"));

        refresh("no-such-client-394", refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    // ---- helpers ------------------------------------------------------------------------------

    /** The redeemed token comes back with a new refresh token, and the old one is then refused. */
    private void assertRotates(String clientId, String refresh) throws Exception {
        MvcResult result = refresh(clientId, refresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andReturn();
        String next = json(result).get("refresh_token").asText();
        assertNotEquals(refresh, next, "refresh tokens rotate (reuseRefreshTokens=false)");

        refresh(clientId, refresh)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));

        refresh(clientId, next).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String clientId, String refresh)
            throws Exception {
        return mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "refresh_token")
                .param("refresh_token", refresh)
                .param("client_id", clientId));
    }

    private String refreshTokenFor(String clientId) throws Exception {
        JsonNode refresh = login(clientId).get("refresh_token");
        assertNotNull(refresh, "login should return a refresh token");
        return refresh.asText();
    }

    private String cliClientId() {
        if (registeredClientRepository.findByClientId(AuthorizationServerConfig.CLI_CLIENT_ID) == null) {
            registeredClientRepository.save(AuthorizationServerConfig.requelCliRegisteredClient());
        }
        return AuthorizationServerConfig.CLI_CLIENT_ID;
    }

    private String registerLoopbackClient(String name) throws Exception {
        String body = "{\"client_name\":\"" + name + "\","
                + "\"redirect_uris\":[\"" + REDIRECT + "\"],"
                + "\"grant_types\":[\"authorization_code\",\"refresh_token\"]}";
        MvcResult result = mockMvc.perform(post("/connect/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("client_id").asText();
    }

    /** Authorize as admin, approve the consent screen, and exchange the code with the PKCE verifier. */
    private JsonNode login(String clientId) throws Exception {
        // The authorization endpoint reads a GET's parameters from the query string.
        String authorize = UriComponentsBuilder.fromPath("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", REDIRECT)
                .queryParam("scope", "mcp")
                .queryParam("code_challenge", CHALLENGE)
                .queryParam("code_challenge_method", "S256")
                .queryParam("state", "s394")
                .encode().build().toUriString();
        MvcResult toConsent = mockMvc.perform(get(authorize).with(user("admin")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String consentState = queryParam(toConsent.getResponse().getRedirectedUrl(), "state");

        MvcResult toClient = mockMvc.perform(post("/oauth2/authorize")
                        .param("client_id", clientId)
                        .param("state", consentState)
                        .param("scope", "mcp")
                        .with(user("admin")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String code = queryParam(toClient.getResponse().getRedirectedUrl(), "code");
        assertNotNull(code, "consent should redirect back to the client with a code");

        MvcResult tokens = mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT)
                        .param("client_id", clientId)
                        .param("code_verifier", VERIFIER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();
        return json(tokens);
    }

    private static String queryParam(String url, String name) {
        assertNotNull(url, "expected a redirect");
        String value = UriComponentsBuilder.fromUri(URI.create(url)).build().getQueryParams().getFirst(name);
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
