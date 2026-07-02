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
package com.rreganjr.requel.service.gateway;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST facade over the {@link CommandGateway} write contract, for out-of-process front-ends (the
 * REST-backed gateway client used by {@code requel-cli}). Unlike the raw {@code /api/commands/*}
 * CQRS endpoint, this dispatches through the in-process {@link CommandGateway} bean, so the gateway
 * <strong>allow/deny policy is enforced server-side</strong> (the denylist boundary — never expose
 * user/identity commands — holds independently of authorization, per doc/local_mcp_bridge.md), not
 * merely trusted to the client.
 *
 * <p>Success returns {@code 200} with the command's result DTO (or empty body when the command has
 * no result). Failure returns a JSON {@link GatewayErrorBody} carrying the exact
 * {@link GatewayException.Kind}, so the client reconstructs a stable error regardless of the HTTP
 * status. Sits under {@code /api/**}, so the standard JWT/PAT/OAuth chain authenticates it and
 * {@code CurrentUserResolver} establishes the acting user.
 */
@RestController
@RequestMapping("/api/gateway/commands")
public class GatewayCommandController {

    private final CommandGateway commandGateway;

    public GatewayCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping(value = "/{commandType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dispatch(
            @PathVariable String commandType,
            @RequestBody(required = false) Map<String, Object> input,
            @RequestHeader(value = "X-Requel-Client", required = false) String clientId) {
        try {
            GatewayResult result = commandGateway.execute(new GatewayRequest(commandType, input, clientId));
            return ResponseEntity.ok(result.result());
        } catch (GatewayException e) {
            return ResponseEntity.status(statusFor(e.getKind()))
                    .body(new GatewayErrorBody(e.getKind().name(), e.getMessage()));
        }
    }

    private static HttpStatus statusFor(GatewayException.Kind kind) {
        return switch (kind) {
            case NOT_ALLOWED, UNAUTHORIZED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case EXECUTION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    /** Error envelope carrying the gateway failure category so clients get a stable {@code kind}. */
    public record GatewayErrorBody(String kind, String message) {
    }
}
