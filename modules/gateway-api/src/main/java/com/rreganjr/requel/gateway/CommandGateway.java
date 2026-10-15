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
package com.rreganjr.requel.gateway;

/**
 * Dispatches a single write command to Requel through the existing CQRS path, after the
 * {@link CommandPolicy} has approved it. The gateway adds reach (MCP tools, CLI, remote
 * connector) over the same command/authorization/audit path the UI uses; it introduces no new
 * write path into the domain.
 * <p>
 * Two implementations are expected: an in-process one (wraps the command factory and handler
 * chain in the same JVM as the application) and a REST-backed one (POSTs to
 * {@code /api/commands/{commandType}} from an out-of-process front-end). Front-ends depend only
 * on this interface.
 */
public interface CommandGateway {

    /**
     * Execute the request's command. Implementations must:
     * <ol>
     *   <li>consult the {@link CommandPolicy} and reject disallowed command types
     *       ({@link GatewayException.Kind#NOT_ALLOWED});</li>
     *   <li>bind the request input to the command's registered input type
     *       ({@link GatewayException.Kind#INVALID_INPUT} on failure);</li>
     *   <li>run it through the authorization-checked command handler chain as the authenticated
     *       user ({@link GatewayException.Kind#UNAUTHORIZED} when the user lacks permission);</li>
     *   <li>return the command's result.</li>
     * </ol>
     *
     * @param request the command type, input, and optional client identity
     * @return the command result
     * @throws GatewayException if the command is not allowed, unknown, invalid, unauthorized, or
     *                          fails during execution
     */
    GatewayResult execute(GatewayRequest request) throws GatewayException;
}
