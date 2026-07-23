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

import java.util.List;
import java.util.Optional;

/**
 * The set of commands the gateway exposes, as {@link CommandDescriptor}s. This is the single
 * source of truth that MCP tools and the CLI generate their surfaces from, so they stay in
 * lockstep with one another and with the allow/deny policy.
 */
public interface GatewayCommandCatalog {

    /** All exposed command descriptors. */
    List<CommandDescriptor> descriptors();

    /** Look up one descriptor by command type. */
    Optional<CommandDescriptor> find(String commandType);

    /**
     * An empty catalog exposing no commands. Useful for read-only deployments and tests where a
     * catalog is required but no write surface should be advertised.
     */
    static GatewayCommandCatalog empty() {
        return new GatewayCommandCatalog() {
            @Override
            public List<CommandDescriptor> descriptors() {
                return List.of();
            }

            @Override
            public Optional<CommandDescriptor> find(String commandType) {
                return Optional.empty();
            }
        };
    }
}
