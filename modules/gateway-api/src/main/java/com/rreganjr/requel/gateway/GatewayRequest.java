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

import java.util.Objects;

/**
 * A request to execute one write command through the {@link CommandGateway}.
 *
 * @param commandType the registered command type string (e.g. {@code "EditGoal"})
 * @param input       the command input payload; either a deserialized input DTO matching the
 *                    command's registered input class, or a loosely-typed map/JSON node the
 *                    gateway implementation will bind to that class. May be {@code null} for
 *                    commands that take no input.
 * @param clientId    optional identifier of the external client making the request (e.g.
 *                    {@code "claude-desktop"}, {@code "requel-cli"}), used for audit attribution
 *                    and per-client rate limiting. May be {@code null}.
 */
public record GatewayRequest(String commandType, Object input, String clientId) {

    public GatewayRequest {
        Objects.requireNonNull(commandType, "commandType");
    }

    /** Convenience for a request with no client identity. */
    public GatewayRequest(String commandType, Object input) {
        this(commandType, input, null);
    }
}
