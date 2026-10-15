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
package com.rreganjr.requel.cli;

import com.rreganjr.requel.gateway.GatewayException;

/**
 * Process exit codes for {@code requel}, chosen so scripts can branch on outcome. Aligns with
 * picocli's conventions ({@code 0} ok, {@code 2} usage) and maps {@link GatewayException.Kind} onto
 * stable codes.
 */
public final class ExitCode {

    /** Command succeeded. */
    public static final int SUCCESS = 0;
    /** Unexpected/internal error. */
    public static final int UNEXPECTED = 1;
    /** Usage error — bad arguments/options (picocli's default). */
    public static final int USAGE = 2;
    /** Authentication/authorization failure (401/403 → {@code UNAUTHORIZED}). */
    public static final int AUTH = 3;
    /** Command not permitted through the gateway ({@code NOT_ALLOWED}). */
    public static final int NOT_ALLOWED = 4;
    /** Request could not be completed: bad input, unknown command, or execution error. */
    public static final int REQUEST_ERROR = 5;

    private ExitCode() {
    }

    /** Map a gateway failure category to an exit code. */
    public static int forKind(GatewayException.Kind kind) {
        return switch (kind) {
            case UNAUTHORIZED -> AUTH;
            case NOT_ALLOWED -> NOT_ALLOWED;
            case NOT_FOUND, INVALID_INPUT, EXECUTION_ERROR -> REQUEST_ERROR;
        };
    }
}
