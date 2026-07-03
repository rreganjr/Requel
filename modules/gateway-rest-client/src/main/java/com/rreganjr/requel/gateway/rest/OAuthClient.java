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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * The OAuth 2.1 authorization-code + PKCE client the {@code requel} CLI drives for interactive
 * {@code requel login} (#83 AS, #70 CLI): discover the AS via RFC 8414 metadata, exchange the
 * authorization code for tokens, and refresh them. Public client — no client secret; PKCE
 * ({@code code_verifier}) is the proof. The browser redirect + loopback capture live in the CLI; this
 * class is just the HTTP calls, so it unit-tests cleanly against a stubbed server.
 */
public class OAuthClient {

    private static final String WELL_KNOWN = "/.well-known/oauth-authorization-server";

    private final RestClient http;

    /** @param baseUrl the Requel server base URL (e.g. {@code http://localhost:8080}). */
    public OAuthClient(String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl).build());
    }

    /** For tests: inject a preconfigured (e.g. MockRestServiceServer-bound) client. */
    OAuthClient(RestClient http) {
        this.http = http;
    }

    /** Fetch the authorization-server metadata (authorization + token endpoints). */
    public AsMetadata discover() {
        return http.get().uri(WELL_KNOWN).retrieve().body(AsMetadata.class);
    }

    /** Exchange an authorization {@code code} (with its PKCE {@code codeVerifier}) for tokens. */
    public OAuthTokens exchangeAuthorizationCode(String tokenEndpoint, String clientId,
            String redirectUri, String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        return post(tokenEndpoint, form);
    }

    /** Redeem a refresh token for a fresh access (and rotated refresh) token. */
    public OAuthTokens refresh(String tokenEndpoint, String clientId, String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        return post(tokenEndpoint, form);
    }

    private OAuthTokens post(String tokenEndpoint, MultiValueMap<String, String> form) {
        Map<String, Object> body = http.post().uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return toTokens(body);
    }

    private static OAuthTokens toTokens(Map<String, Object> body) {
        if (body == null || body.get("access_token") == null) {
            throw new IllegalStateException("Token response missing access_token");
        }
        String accessToken = String.valueOf(body.get("access_token"));
        Object refresh = body.get("refresh_token");
        Object scope = body.get("scope");
        long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 0L;
        Instant expiresAt = Instant.now().plus(Duration.ofSeconds(expiresIn));
        return new OAuthTokens(accessToken,
                refresh == null ? null : String.valueOf(refresh),
                scope == null ? null : String.valueOf(scope),
                expiresAt);
    }
}
