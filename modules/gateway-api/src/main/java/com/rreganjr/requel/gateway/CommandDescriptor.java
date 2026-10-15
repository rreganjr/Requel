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
 * Describes a gateway-exposed command so front-ends can generate their surfaces from one source
 * of truth: MCP tool schemas, generated CLI subcommands, and help text. This is the "schema, not
 * just a predicate" the front-ends need.
 * <p>
 * Descriptors are built (in the implementation module) from the existing
 * {@code CommandRegistration} entries plus gateway metadata; the {@code inputType} is the
 * command's registered input DTO class, from which a JSON schema can be derived.
 *
 * @param commandType        the registered command type string (e.g. {@code "EditGoal"})
 * @param inputType          the input DTO class for this command ({@code Void.class} if none)
 * @param title              a short human-readable label
 * @param description        a longer description of what the command does
 * @param write              {@code true} if the command mutates state (gated by the write opt-in
 *                           flag); {@code false} for read-only/query commands
 * @param authorizationHint  a human-readable hint of the permission the command requires
 *                           (e.g. {@code "Goal[Edit]"}); informational, not enforcement
 */
public record CommandDescriptor(
        String commandType,
        Class<?> inputType,
        String title,
        String description,
        boolean write,
        String authorizationHint
) {
}
