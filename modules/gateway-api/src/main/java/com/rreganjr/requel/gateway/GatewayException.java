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
 * Thrown when a gateway request cannot be completed. The {@link Kind} lets a front-end map the
 * failure onto an appropriate protocol error (e.g. a JSON-RPC error code or an HTTP status).
 */
public class GatewayException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The category of failure, so callers can map it to a stable error code. */
    public enum Kind {
        /** The command type is not permitted through the gateway (denylist / not on allowlist). */
        NOT_ALLOWED,
        /** The command type is unknown / not registered. */
        NOT_FOUND,
        /** The input payload was missing or malformed for the command. */
        INVALID_INPUT,
        /** The authenticated user lacks permission for the command. */
        UNAUTHORIZED,
        /** The command failed during execution. */
        EXECUTION_ERROR
    }

    private final Kind kind;

    public GatewayException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public GatewayException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
