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
package com.rreganjr.requel.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

/**
 * 401 entry point for the MCP resource server that adds the RFC 9728 {@code resource_metadata}
 * parameter to the {@code WWW-Authenticate} header (issue #83, Slice 3).
 *
 * <p>MCP agent clients follow the MCP authorization spec: on a 401 they read {@code resource_metadata}
 * from {@code WWW-Authenticate}, fetch that document
 * ({@link ProtectedResourceMetadataController#WELL_KNOWN_PATH}), discover the authorization server,
 * and run the OAuth flow. This entry point delegates to the standard
 * {@link BearerTokenAuthenticationEntryPoint} (so status and any {@code error}/{@code error_description}
 * details are formatted exactly as Spring's resource server would) and then appends the
 * {@code resource_metadata} parameter pointing at this server's metadata document.
 */
public class McpAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // Let the standard bearer entry point set the status and the base WWW-Authenticate header.
        delegate.commence(request, response, authException);

        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String metadataUrl = base + ProtectedResourceMetadataController.WELL_KNOWN_PATH;
        String param = "resource_metadata=\"" + metadataUrl + "\"";

        String existing = response.getHeader(HttpHeaders.WWW_AUTHENTICATE);
        String updated;
        if (existing == null || existing.isBlank()) {
            updated = "Bearer " + param;
        } else if (existing.equals("Bearer")) {
            // No params yet (missing-token case) — RFC 7235 auth-param is space-separated here.
            updated = "Bearer " + param;
        } else {
            // Existing params (e.g. error="invalid_token", ...) — append comma-separated.
            updated = existing + ", " + param;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, updated);
    }
}
