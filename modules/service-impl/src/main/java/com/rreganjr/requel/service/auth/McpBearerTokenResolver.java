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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Routes a {@code Bearer} credential to the MCP OAuth2 resource server <em>only</em> when it is an
 * authorization-server-issued JWT (issue #83, Slice 2). This lets three credential kinds coexist on
 * {@code /api/mcp/**} without conflicting:
 *
 * <ul>
 *   <li><b>OAuth access token</b> — an AS-issued JWS signed with an asymmetric key (RS/PS/ES). This
 *       resolver returns it, so the resource server's bearer filter validates it.</li>
 *   <li><b>Personal access token (#73)</b> — opaque, prefixed {@code reqpat_}. Returned as
 *       {@code null} here so the resource server ignores it; {@link JwtAuthenticationFilter}
 *       (placed before the bearer filter) handles the PAT.</li>
 *   <li><b>SPA login JWT</b> — a symmetric {@code HS*} JWS. Returned as {@code null} here so the
 *       resource server ignores it; {@link JwtAuthenticationFilter} validates it as before.</li>
 * </ul>
 *
 * <p>The alg family is read from the (unverified) JWS header purely to <em>route</em> the token to
 * the correct authenticator; the actual signature/validity check is done downstream by the chosen
 * authenticator, so a forged header only changes which validator rejects the token.
 */
public class McpBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String token = delegate.resolve(request);
        if (token == null) {
            return null;
        }
        if (ApiTokenService.isApiToken(token)) {
            return null; // PAT — handled by JwtAuthenticationFilter.
        }
        // Only AS-issued (asymmetric-signed) JWTs go to the resource server; HS* login JWTs do not.
        return isAsymmetricJws(token) ? token : null;
    }

    private static boolean isAsymmetricJws(String token) {
        int dot = token.indexOf('.');
        if (dot <= 0) {
            return false;
        }
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(token.substring(0, dot));
            String header = new String(headerBytes, StandardCharsets.UTF_8);
            // AS access tokens use RS256 by default; accept the asymmetric JWS families (RS/PS/ES).
            return header.matches("(?s).*\"alg\"\\s*:\\s*\"(RS|PS|ES)\\d+\".*");
        } catch (RuntimeException e) {
            return false;
        }
    }
}
