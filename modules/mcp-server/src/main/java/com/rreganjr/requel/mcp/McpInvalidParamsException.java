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
package com.rreganjr.requel.mcp;

/**
 * Thrown when an MCP JSON-RPC call has structurally valid routing (a known method) but
 * invalid parameters — a missing/mistyped required field, an unknown tool name, or an
 * unsupported resource URI. Mapped to JSON-RPC error code {@code -32602} (Invalid params),
 * distinct from {@code -32603} (Internal error) used for genuine server faults.
 */
class McpInvalidParamsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	McpInvalidParamsException(String message) {
		super(message);
	}
}
