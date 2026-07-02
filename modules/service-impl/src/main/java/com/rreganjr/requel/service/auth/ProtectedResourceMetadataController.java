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

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 9728 Protected Resource Metadata for the MCP resource (issue #83, Slice 3).
 *
 * <p>Serves {@code /.well-known/oauth-protected-resource} so an MCP agent client, on a 401 from
 * {@code /api/mcp/**}, can discover which authorization server protects the resource and then run
 * the OAuth discovery + authorization-code/PKCE flow — no pre-shared token or manual config.
 * {@link McpAuthenticationEntryPoint} points the {@code WWW-Authenticate: Bearer resource_metadata}
 * parameter at this document.
 *
 * <p>The endpoint is intentionally public: it is matched by no security chain (not {@code /api/**},
 * not an AS endpoint, not {@code /login}), so it is served without authentication. Both the bare
 * well-known path and the RFC 9728 resource-path-suffixed variant are mapped to the same document,
 * for clients that use either form.
 */
@RestController
public class ProtectedResourceMetadataController {

    /** Well-known path for the MCP protected-resource metadata document. */
    public static final String WELL_KNOWN_PATH = "/.well-known/oauth-protected-resource";

    /** Path (relative to the app base) of the MCP resource this metadata describes. */
    private static final String MCP_RESOURCE_PATH = "/api/mcp";

    private final AuthorizationServerSettings authorizationServerSettings;

    public ProtectedResourceMetadataController(AuthorizationServerSettings authorizationServerSettings) {
        this.authorizationServerSettings = authorizationServerSettings;
    }

    @GetMapping({WELL_KNOWN_PATH, WELL_KNOWN_PATH + MCP_RESOURCE_PATH})
    public Map<String, Object> metadata() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String issuer = (authorizationServerSettings.getIssuer() != null)
                ? authorizationServerSettings.getIssuer()
                : base;

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("resource", base + MCP_RESOURCE_PATH);
        document.put("authorization_servers", List.of(issuer));
        document.put("scopes_supported", List.of("mcp"));
        document.put("bearer_methods_supported", List.of("header"));
        return document;
    }
}
