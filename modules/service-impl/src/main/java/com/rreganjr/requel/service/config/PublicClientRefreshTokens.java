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

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Refresh tokens for public clients (issue #394).
 *
 * <p>Every Requel OAuth client is public (PKCE, no secret): {@code requel-cli} and the MCP clients
 * that register themselves. Spring Authorization Server doesn't support refresh tokens for them out
 * of the box, in two places:
 * <ul>
 *   <li>its {@code OAuth2RefreshTokenGenerator} returns no refresh token for a public client on the
 *       authorization-code grant, so the login response carries none;</li>
 *   <li>its public-client authentication only recognises a PKCE code exchange (it needs a
 *       {@code code_verifier}), so a {@code refresh_token} request from a public client has no way
 *       to authenticate.</li>
 * </ul>
 * Without both, an OAuth session ends with its 1-hour access token. OAuth 2.1 (section 4.3.1)
 * allows refresh tokens for public clients when they rotate, and Requel's do: every client gets
 * {@code reuseRefreshTokens(false)}, so each refresh token works once.
 *
 * <p>A public client on the refresh grant is identified by its {@code client_id} alone. The refresh
 * token is the credential; Spring's refresh provider still checks it belongs to that client.
 */
final class PublicClientRefreshTokens {

    private PublicClientRefreshTokens() {
    }

    /**
     * Issues a refresh token whenever one is asked for, public client or not. Otherwise the same as
     * Spring's {@code OAuth2RefreshTokenGenerator}: 96 random bytes, the client's refresh TTL. Spring
     * asks only when the client has the {@code refresh_token} grant.
     */
    static final class Generator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

        private final StringKeyGenerator keys =
                new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

        @Override
        public OAuth2RefreshToken generate(OAuth2TokenContext context) {
            if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
                return null;
            }
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(
                    context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
            return new OAuth2RefreshToken(keys.generateKey(), issuedAt, expiresAt);
        }
    }

    /**
     * Turns a {@code refresh_token} request that names a client but presents no client credentials
     * into a public-client authentication request. Anything else (a secret, a client assertion, a
     * Basic header, another grant) is left to Spring's own converters.
     */
    static final class Converter implements AuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            if (!HttpMethod.POST.matches(request.getMethod())
                    || !AuthorizationGrantType.REFRESH_TOKEN.getValue()
                            .equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                    || request.getHeader(HttpHeaders.AUTHORIZATION) != null
                    || request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null
                    || request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION) != null) {
                return null;
            }
            String[] clientIds = request.getParameterValues(OAuth2ParameterNames.CLIENT_ID);
            if (clientIds == null || clientIds.length == 0 || clientIds[0].isBlank()) {
                return null;
            }
            if (clientIds.length != 1) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: client_id", null));
            }
            return new OAuth2ClientAuthenticationToken(clientIds[0], ClientAuthenticationMethod.NONE,
                    null, Map.of(OAuth2ParameterNames.GRANT_TYPE,
                            AuthorizationGrantType.REFRESH_TOKEN.getValue()));
        }
    }

    /**
     * Authenticates the request {@link Converter} built: the client must exist, be public and have
     * the {@code refresh_token} grant. Leaves every other client authentication to Spring.
     */
    static final class Provider implements AuthenticationProvider {

        private final RegisteredClientRepository registeredClientRepository;

        Provider(RegisteredClientRepository registeredClientRepository) {
            this.registeredClientRepository = registeredClientRepository;
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            OAuth2ClientAuthenticationToken request = (OAuth2ClientAuthenticationToken) authentication;
            if (!ClientAuthenticationMethod.NONE.equals(request.getClientAuthenticationMethod())
                    || !AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(
                            request.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE))) {
                return null;
            }
            RegisteredClient client = registeredClientRepository.findByClientId(
                    String.valueOf(request.getPrincipal()));
            if (client == null
                    || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                    || !client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT,
                        "Client authentication failed: client_id", null));
            }
            return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}
