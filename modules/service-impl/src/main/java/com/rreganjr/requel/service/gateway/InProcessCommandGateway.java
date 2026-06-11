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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.CommandPolicy;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.PolicyDecision;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.command.ApiCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * In-process {@link CommandGateway}: runs a write command through the exact same CQRS path the
 * REST {@code CommandController} uses — the shared {@code commandHandler} chain (audit →
 * current-user → retry → exception-mapping → authorization → analysis → default) and the
 * {@link ApiCommandFactory} for input binding and result extraction. It introduces no new write
 * path into the domain; it only adds the gateway allow/deny boundary in front of it and maps
 * failures onto {@link GatewayException.Kind} so any transport (MCP, CLI) can render a stable
 * error.
 * <p>
 * The command executes as the current Spring Security user: {@code CurrentUserCommandHandler}
 * inside the chain stamps {@code editedBy} from the {@code SecurityContext}, and
 * {@code AuthorizingCommandHandler} enforces per-stakeholder permissions. Callers are responsible
 * for establishing that security context (HTTP filter for remote clients; an explicit context for
 * stdio/tests — wired in Slice 5).
 * <p>
 * TODO(Slice 4): publish the same SSE change events the controller emits
 * ({@code publishProjectChangedIfScoped} / {@code publishEntityChangedIfPresent}) when the MCP
 * transport is wired, so external writes refresh open UI sessions.
 */
@Service
public class InProcessCommandGateway implements CommandGateway {

    private static final Logger log = LoggerFactory.getLogger(InProcessCommandGateway.class);

    private final CommandPolicy policy;
    private final CommandRegistry registry;
    private final ApiCommandFactory apiCommandFactory;
    private final CommandHandler commandHandler;
    private final ObjectMapper objectMapper;

    public InProcessCommandGateway(CommandPolicy policy, CommandRegistry registry,
            ApiCommandFactory apiCommandFactory, CommandHandler commandHandler,
            ObjectMapper objectMapper) {
        this.policy = policy;
        this.registry = registry;
        this.apiCommandFactory = apiCommandFactory;
        this.commandHandler = commandHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayResult execute(GatewayRequest request) throws GatewayException {
        String commandType = request.commandType();

        // 1. Unknown command type -> NOT_FOUND (distinct from a known-but-denied type).
        if (!registry.isRegistered(commandType)) {
            throw new GatewayException(GatewayException.Kind.NOT_FOUND,
                    "Unknown command type: " + commandType);
        }

        // 2. Type-level policy gate on the raw input (denylist / not-on-allowlist).
        requireAllowed(commandType, request.input());

        // 3. Bind the raw input to the command's registered input DTO type.
        Object boundInput = bindInput(commandType, request.input());

        // 4. Input-aware policy gate on the bound input (e.g. non-user-stakeholder delete guard).
        requireAllowed(commandType, boundInput);

        // 5. Create, execute through the shared handler chain, and extract the result DTO.
        try {
            Command command = apiCommandFactory.newCommand(commandType, boundInput);
            commandHandler.execute(command);
            Object result = apiCommandFactory.extractResult(commandType, command);
            return new GatewayResult(commandType, result);
        } catch (AuthorizationException e) {
            throw new GatewayException(GatewayException.Kind.UNAUTHORIZED, e.getMessage(), e);
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gateway command '{}' failed: {}", commandType, e.getMessage());
            throw new GatewayException(GatewayException.Kind.EXECUTION_ERROR,
                    "Command '" + commandType + "' failed: " + e.getMessage(), e);
        }
    }

    private void requireAllowed(String commandType, Object input) throws GatewayException {
        PolicyDecision decision = policy.evaluate(commandType, input);
        if (!decision.allowed()) {
            throw new GatewayException(GatewayException.Kind.NOT_ALLOWED, decision.reason());
        }
    }

    /**
     * Bind {@code rawInput} to the command's registered input type. Accepts an already-typed DTO,
     * a loosely-typed map/JSON node (converted via Jackson), or {@code null}. Commands with no
     * input ({@code Void.class}) bind to {@code null}.
     */
    private Object bindInput(String commandType, Object rawInput) throws GatewayException {
        Class<?> inputType = apiCommandFactory.getInputType(commandType);
        if (inputType == Void.class || rawInput == null) {
            return null;
        }
        if (inputType.isInstance(rawInput)) {
            return rawInput;
        }
        try {
            return objectMapper.convertValue(rawInput, inputType);
        } catch (IllegalArgumentException e) {
            throw new GatewayException(GatewayException.Kind.INVALID_INPUT,
                    "Input for command '" + commandType + "' could not be bound to "
                            + inputType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
