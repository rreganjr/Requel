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
 * Holds the optional per-client identifier for the current MCP request thread (issue #69 Slice 5).
 * Populated by {@link McpClientContextFilter} from the {@code X-Requel-Client} header and read by
 * the tool dispatch so it can be carried into {@code GatewayRequest.clientId} and the rate-limit
 * hook. A thread-local seam: real per-client identities (mapped to users) arrive with the
 * API-key/PAT work (#73). The MCP request executes on the servlet thread, so a thread-local is
 * sufficient for the WebMVC/JSON-RPC transports; the stdio bridge (#71/Slice 7) will set it
 * explicitly.
 */
public final class McpClientContext {

	private static final ThreadLocal<String> CLIENT_ID = new ThreadLocal<>();

	private McpClientContext() {
	}

	public static void setClientId(String clientId) {
		CLIENT_ID.set(clientId);
	}

	public static String clientId() {
		return CLIENT_ID.get();
	}

	public static void clear() {
		CLIENT_ID.remove();
	}
}
