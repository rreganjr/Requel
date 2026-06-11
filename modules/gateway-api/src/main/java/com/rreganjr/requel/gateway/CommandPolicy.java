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
 * Decides whether a command type (and optionally its input) may be dispatched through the
 * gateway. This is the gateway-level guard that is independent of, and additional to, the
 * domain's own command authorization: it enforces the allow/deny boundary (e.g. never expose
 * user/identity commands) before a command reaches the handler chain.
 * <p>
 * The {@code input} is provided so input-aware guards can be layered on — for example, allowing
 * {@code DeleteStakeholder} only for non-user stakeholders. Implementations that only key on the
 * command type may ignore it.
 */
public interface CommandPolicy {

    /**
     * @param commandType the registered command type string
     * @param input       the request input payload (may be {@code null})
     * @return an allow or deny decision
     */
    PolicyDecision evaluate(String commandType, Object input);
}
