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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loopback-restricted anonymous Dynamic Client Registration (issue #238).
 *
 * <p>Spring Authorization Server's OIDC client-registration endpoint ({@code POST /connect/register})
 * is gated: it requires an initial access token minted from the {@code requel-registrar} client, and
 * it does not support anonymous RFC 7591 registration natively. Interactive agent clients such as the
 * Codex CLI perform <em>anonymous</em> DCR and cannot supply an initial token or a pre-registered
 * {@code client_id}, so they cannot connect over OAuth. This filter adds a narrow, opt-in
 * ({@code requel.oauth.dcr.allow-anonymous-loopback=true}) path that lets such a client self-register
 * <em>only</em> when the request is anonymous, originates from a loopback peer, and every redirect URI
 * is loopback — reusing the exact same client policy as gated DCR
 * ({@link AuthorizationServerConfig#buildLoopbackMcpClient}).
 *
 * <p>The filter is wired <b>before</b> the resource-server bearer filter on the AS chain and only
 * handles the anonymous-loopback case; any request carrying an {@code Authorization} header (the
 * gated / initial-access-token path) and any request from a non-loopback peer are passed straight
 * through to Spring AS unchanged. Consent + Requel login are still required at the authorize hop, and
 * the per-stakeholder gateway authorization is unchanged — a registered client only ever acts as the
 * user who logs in and consents.
 */
final class AnonymousLoopbackDcrFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AnonymousLoopbackDcrFilter.class);

    /** Spring AS's default OIDC client-registration endpoint path. */
    private static final String REGISTRATION_PATH = "/connect/register";

    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    AnonymousLoopbackDcrFilter(RegisteredClientRepository registeredClientRepository,
            ObjectMapper objectMapper) {
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !("POST".equalsIgnoreCase(request.getMethod())
                && uri != null && uri.endsWith(REGISTRATION_PATH));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Only the ANONYMOUS case is handled here. A request carrying an Authorization header is the
        // gated / initial-access-token path — hand it to Spring AS untouched.
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        // Anonymous registration is permitted only from a loopback peer (defense in depth for an AS
        // bound to a non-loopback interface). A remote anonymous caller falls through to Spring AS,
        // which returns its normal 401.
        if (!isLoopbackPeer(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            JsonNode body = objectMapper.readTree(request.getInputStream());
            String clientName = (body != null && body.hasNonNull("client_name"))
                    ? body.get("client_name").asText() : null;
            List<String> redirectUris = new ArrayList<>();
            if (body != null) {
                JsonNode uris = body.get("redirect_uris");
                if (uris != null && uris.isArray()) {
                    uris.forEach(node -> redirectUris.add(node.asText()));
                }
            }
            // Shared policy: loopback validation + public/PKCE + consent + scope mcp + 1h/30d tokens.
            RegisteredClient client =
                    AuthorizationServerConfig.buildLoopbackMcpClient(clientName, redirectUris);
            registeredClientRepository.save(client);
            log.info("Anonymous loopback DCR registered client '{}' (name='{}', redirectUris={}).",
                    client.getClientId(), client.getClientName(), client.getRedirectUris());
            writeRegistrationResponse(response, client);
        } catch (OAuth2AuthenticationException ex) {
            OAuth2Error error = ex.getError();
            writeError(response, error.getErrorCode(), error.getDescription());
        }
    }

    private static boolean isLoopbackPeer(HttpServletRequest request) {
        try {
            String remoteAddr = request.getRemoteAddr();
            // remoteAddr is an IP literal, so getByName does not perform a DNS lookup.
            return remoteAddr != null && InetAddress.getByName(remoteAddr).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * RFC 7591 §3.2.1 client-information (success) response. The exact field set required by the real
     * clients (codex / Claude Code) is confirmed by the issue #238 Step 1 spike; this covers the
     * standard fields a public/PKCE loopback client needs to proceed to the authorize step.
     */
    private void writeRegistrationResponse(HttpServletResponse response, RegisteredClient client)
            throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("client_id", client.getClientId());
        Instant issuedAt = client.getClientIdIssuedAt();
        if (issuedAt != null) {
            payload.put("client_id_issued_at", issuedAt.getEpochSecond());
        }
        if (client.getClientName() != null) {
            payload.put("client_name", client.getClientName());
        }
        payload.put("redirect_uris", new ArrayList<>(client.getRedirectUris()));
        List<String> grantTypes = new ArrayList<>();
        client.getAuthorizationGrantTypes().forEach(grant -> grantTypes.add(grant.getValue()));
        payload.put("grant_types", grantTypes);
        // Public / PKCE client — no secret issued.
        payload.put("token_endpoint_auth_method", "none");
        payload.put("scope", String.join(" ", client.getScopes()));
        response.setStatus(HttpStatus.CREATED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), payload);
    }

    /** RFC 7591 §3.2.2 client-registration error response. */
    private void writeError(HttpServletResponse response, String error, String description)
            throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", error);
        if (description != null) {
            payload.put("error_description", description);
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), payload);
    }
}
