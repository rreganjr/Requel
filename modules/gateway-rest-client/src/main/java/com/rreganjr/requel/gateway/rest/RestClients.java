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

import org.springframework.web.client.RestClient;

/** Shared construction of the bearer-authenticated {@link RestClient} used by the REST gateways. */
final class RestClients {

    private RestClients() {
    }

    /**
     * A {@link RestClient} against {@code baseUrl} that attaches {@code Authorization: Bearer <token>}
     * from {@code tokenSource} on every request (resolved per request, so an OAuth caller can refresh
     * behind the source). No header is sent when the source yields {@code null}/blank.
     */
    static RestClient bearer(String baseUrl, BearerTokenSource tokenSource) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    String token = tokenSource.currentToken();
                    if (token != null && !token.isBlank()) {
                        request.getHeaders().setBearerAuth(token);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
