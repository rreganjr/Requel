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

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * Fetches the server's write-command catalog from
 * {@code GET /api/gateway/commands/descriptors}. This is how out-of-process front-ends (e.g.
 * {@code requel commands}) discover the invocable command surface at runtime, so the CLI reflects
 * exactly what the running server exposes — including that the list is <em>empty</em> when the
 * server has writes disabled ({@code requel.gateway.write.enabled=false}), mirroring how the MCP
 * server hides its write tools.
 *
 * <p>On an HTTP error the underlying {@link RestClient} throws
 * {@link org.springframework.web.client.RestClientResponseException} (unchecked), which a front-end
 * maps to an exit code / message.
 */
public class RestGatewayCatalog {

    private final RestClient http;

    public RestGatewayCatalog(String baseUrl, BearerTokenSource tokenSource) {
        this(RestClients.bearer(baseUrl, tokenSource));
    }

    /** For tests: inject a preconfigured (e.g. MockRestServiceServer-bound) client. */
    RestGatewayCatalog(RestClient http) {
        this.http = http;
    }

    /** @return the write commands the server currently exposes; empty when writes are disabled. */
    public List<CommandInfo> descriptors() {
        return http.get().uri("/api/gateway/commands/descriptors")
                .retrieve().body(new ParameterizedTypeReference<>() { });
    }
}
