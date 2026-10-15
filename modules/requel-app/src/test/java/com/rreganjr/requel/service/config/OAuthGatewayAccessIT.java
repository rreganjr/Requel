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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rreganjr.AbstractIntegrationTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #390: an authorization-server-issued access token - what {@code requel-cli login --oauth} stores -
 * authenticates the REST gateway the CLI calls ({@code /api/gateway/**}), not only {@code /api/mcp}.
 * Before, the gateway sat on the {@code /api/**} chain, which knows only login JWTs and PATs, so the
 * OAuth login succeeded and every CLI call after it was a 401.
 *
 * <p>The token is minted with the server's own signing key, as the authorization server would after
 * the browser login and consent.
 */
@AutoConfigureMockMvc
public class OAuthGatewayAccessIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    private String oauthToken(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost")
                .subject(username)
                .audience(java.util.List.of("requel-cli"))
                .claim("scope", "mcp")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new NimbusJwtEncoder(jwkSource).encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    @Test
    void anOAuthAccessTokenReadsThroughTheGateway() throws Exception {
        mockMvc.perform(get("/api/gateway/query/projects")
                        .header("Authorization", "Bearer " + oauthToken("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void anOAuthAccessTokenListsTheGatewayCommands() throws Exception {
        mockMvc.perform(get("/api/gateway/commands/descriptors")
                        .header("Authorization", "Bearer " + oauthToken("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void theGatewayStillRefusesAnonymousCalls() throws Exception {
        mockMvc.perform(get("/api/gateway/query/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anOAuthTokenForAnUnknownUserIsRefused() throws Exception {
        mockMvc.perform(get("/api/gateway/query/projects")
                        .header("Authorization", "Bearer " + oauthToken("no-such-user-390")))
                .andExpect(status().isUnauthorized());
    }

    /** OAuth tokens stay scoped to the MCP and gateway surfaces; the SPA's REST API doesn't take them. */
    @Test
    void anOAuthAccessTokenIsNotAcceptedByTheRestOfTheApi() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + oauthToken("admin")))
                .andExpect(status().isUnauthorized());
    }
}
