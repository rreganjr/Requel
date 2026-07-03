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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The subset of RFC 8414 authorization-server metadata the CLI needs, from
 * {@code GET /.well-known/oauth-authorization-server}: where to send the user to authorize and where
 * to exchange/refresh tokens. Unknown fields are ignored.
 *
 * @param issuer                the AS issuer identifier
 * @param authorizationEndpoint the authorization endpoint URL (browser)
 * @param tokenEndpoint         the token endpoint URL (code exchange + refresh)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AsMetadata(
        @JsonProperty("issuer") String issuer,
        @JsonProperty("authorization_endpoint") String authorizationEndpoint,
        @JsonProperty("token_endpoint") String tokenEndpoint) {
}
