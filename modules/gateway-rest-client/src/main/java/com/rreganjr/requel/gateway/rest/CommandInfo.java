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
package com.rreganjr.requel.gateway.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Client-side view of a gateway command descriptor, as returned by
 * {@code GET /api/gateway/commands/descriptors}. Mirrors the server's {@code DescriptorView}: the
 * input type is a simple class name (a {@link String}), not a {@code Class}, since the client has no
 * access to the server's DTO classes. Decoupled from {@link com.rreganjr.requel.gateway.CommandDescriptor}
 * (which carries a {@code Class<?>} that can't be JSON-deserialized here) so it can be a plain
 * transport record.
 *
 * @param commandType       the registered command type string (e.g. {@code "EditGoal"})
 * @param inputType         the input DTO's simple class name, or {@code null} if none
 * @param title             a short human-readable label
 * @param description       a longer description, or {@code null}
 * @param write             {@code true} for state-mutating commands
 * @param authorizationHint a human-readable permission hint, or {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommandInfo(
        String commandType,
        String inputType,
        String title,
        String description,
        boolean write,
        String authorizationHint
) {
}
