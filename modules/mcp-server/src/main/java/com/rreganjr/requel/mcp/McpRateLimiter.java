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
 * SPI seam for per-client rate limiting of MCP tool calls (issue #69 Slice 5). Invoked once per
 * tool call at the single chokepoint ({@link McpReadService#callTool}), so it covers every
 * transport (the hand-rolled JSON-RPC server and the Spring AI MCP server). The default
 * {@link NoOpMcpRateLimiter} allows everything; a real limiter (e.g. token bucket keyed by client
 * or authenticated user) can be dropped in later as a {@code @Primary} bean with no call-site
 * change.
 */
@FunctionalInterface
public interface McpRateLimiter {

	/** Allow-all limiter, used by the read-only/test constructors. */
	McpRateLimiter NOOP = (clientId, toolName) -> {
	};

	/**
	 * @param clientId optional per-client identifier (from the {@code X-Requel-Client} header),
	 *                 may be {@code null}
	 * @param toolName the MCP tool being invoked
	 * @throws McpRateLimitExceededException if the call should be rejected
	 */
	void check(String clientId, String toolName) throws McpRateLimitExceededException;
}
