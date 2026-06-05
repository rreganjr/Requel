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
 * The outcome of a {@link CommandGateway#execute(GatewayRequest)} call.
 *
 * @param commandType the command type that was executed
 * @param result      the command's result payload (typically an API DTO produced by the
 *                    command's registered result extractor), or {@code null} if the command
 *                    produces no result
 */
public record GatewayResult(String commandType, Object result) {

    public GatewayResult {
        Objects.requireNonNull(commandType, "commandType");
    }
}
