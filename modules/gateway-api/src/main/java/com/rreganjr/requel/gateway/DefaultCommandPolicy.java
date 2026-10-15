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

import java.util.Set;

/**
 * Default, data-driven {@link CommandPolicy}: <strong>default-deny</strong> with an explicit
 * allowlist and an explicit denylist. The denylist always wins, so user/identity commands placed
 * on it can never be exposed even if mistakenly added to the allowlist. A command type that is on
 * neither list is denied.
 * <p>
 * This implementation keys only on the command type. Input-aware guards (e.g. permitting
 * {@code DeleteStakeholder} only for non-user stakeholders) are layered as a decorating
 * {@link CommandPolicy} in the implementation module, where the concrete input types are visible.
 */
public final class DefaultCommandPolicy implements CommandPolicy {

    private final Set<String> allowed;
    private final Set<String> denied;

    /**
     * @param allowed command types permitted through the gateway
     * @param denied  command types that must never be exposed (wins over {@code allowed})
     */
    public DefaultCommandPolicy(Set<String> allowed, Set<String> denied) {
        this.allowed = Set.copyOf(allowed);
        this.denied = Set.copyOf(denied);
    }

    @Override
    public PolicyDecision evaluate(String commandType, Object input) {
        if (commandType == null) {
            return PolicyDecision.deny("no command type");
        }
        if (denied.contains(commandType)) {
            return PolicyDecision.deny("command '" + commandType + "' is denylisted");
        }
        if (allowed.contains(commandType)) {
            return PolicyDecision.allow();
        }
        return PolicyDecision.deny("command '" + commandType + "' is not on the gateway allowlist");
    }

    public Set<String> allowed() {
        return allowed;
    }

    public Set<String> denied() {
        return denied;
    }
}
