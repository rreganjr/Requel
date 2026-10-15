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
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * REST-backed {@link CommandGateway}: dispatches write commands to a running Requel via
 * {@code POST /api/gateway/commands/{commandType}} — the server-side gateway facade
 * ({@code GatewayCommandController}), so the allow/deny policy and per-stakeholder authorization are
 * enforced <em>server-side</em> (the client never has to be trusted with the boundary). Out-of-process
 * front-ends such as {@code requel-cli} use this to reuse the same command/authorization/audit path
 * as the UI.
 *
 * <p>The request input is serialized as the JSON body; the server binds it to the command's
 * registered input DTO. On failure the server returns a JSON body carrying the exact
 * {@link GatewayException.Kind}, which is reconstructed here (falling back to an HTTP-status mapping
 * if the body is absent). An {@code X-Requel-Client} header carries the client id for audit
 * attribution (defaults to {@code requel-cli}); authentication is whatever {@link BearerTokenSource}
 * yields — a PAT (#73) or an OAuth access token (#83).
 */
public class RestCommandGateway implements CommandGateway {

    /** Default client id sent for audit attribution when the request carries none. */
    static final String DEFAULT_CLIENT_ID = "requel-cli";

    private final RestClient http;

    public RestCommandGateway(String baseUrl, BearerTokenSource tokenSource) {
        this(RestClients.bearer(baseUrl, tokenSource));
    }

    /** For tests: inject a preconfigured (e.g. MockRestServiceServer-bound) client. */
    RestCommandGateway(RestClient http) {
        this.http = http;
    }

    @Override
    public GatewayResult execute(GatewayRequest request) throws GatewayException {
        String clientId = request.clientId() != null ? request.clientId() : DEFAULT_CLIENT_ID;
        Object input = request.input() != null ? request.input() : Map.of();

        try {
            Object result = http.post()
                    .uri("/api/gateway/commands/{commandType}", request.commandType())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Requel-Client", clientId)
                    .body(input)
                    .retrieve()
                    .body(Object.class);
            return new GatewayResult(request.commandType(), result);

        } catch (RestClientResponseException e) {
            throw toGatewayException(e);
        } catch (RestClientException e) {
            // Connection refused, timeout, malformed response, etc.
            throw new GatewayException(GatewayException.Kind.EXECUTION_ERROR,
                    "Failed to reach Requel: " + e.getMessage(), e);
        }
    }

    /**
     * Reconstruct the gateway failure from the server's error body (authoritative {@code kind}),
     * falling back to a status-code mapping if the body isn't the expected shape (e.g. a 401 with an
     * empty body from the security layer, before the request reaches the gateway controller).
     */
    private static GatewayException toGatewayException(RestClientResponseException e) {
        try {
            GatewayErrorBody body = e.getResponseBodyAs(GatewayErrorBody.class);
            if (body != null && body.kind() != null) {
                return new GatewayException(GatewayException.Kind.valueOf(body.kind()),
                        body.message() != null ? body.message() : e.getStatusText(), e);
            }
        } catch (RuntimeException ignored) {
            // Body wasn't a GatewayErrorBody (or kind was not a known enum name) — fall back.
        }
        return new GatewayException(kindFor(e.getStatusCode()), fallbackMessage(e), e);
    }

    private static GatewayException.Kind kindFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> GatewayException.Kind.INVALID_INPUT;
            case 401, 403 -> GatewayException.Kind.UNAUTHORIZED;
            case 404 -> GatewayException.Kind.NOT_FOUND;
            default -> GatewayException.Kind.EXECUTION_ERROR;
        };
    }

    private static String fallbackMessage(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return (body != null && !body.isBlank()) ? body : e.getStatusText();
    }

    /** Mirror of {@code GatewayCommandController.GatewayErrorBody}; decoupled to avoid a dependency. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayErrorBody(String kind, String message) {
    }
}
