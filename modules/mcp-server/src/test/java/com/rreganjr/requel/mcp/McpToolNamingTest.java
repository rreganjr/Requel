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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the MCP tool-name convention (issue #82). MCP tool names must match
 * {@code ^[a-zA-Z0-9_-]{1,64}$} — dots are not allowed — and a dotted prefix (the old
 * {@code requel.*}) caused spec-compliant clients (Cursor, Claude Code) to reject the tools. This
 * asserts every advertised tool name (read + write) stays within the pattern so the regression
 * can't return.
 */
class McpToolNamingTest {

	private static final java.util.regex.Pattern MCP_TOOL_NAME =
			java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@SuppressWarnings("unchecked")
	void allAdvertisedToolNamesMatchTheMcpPattern() {
		// Write-enabled so the write tools are advertised and checked too.
		McpWriteService writes = new McpWriteService(
				request -> new GatewayResult(request.commandType(), null),
				McpTestCatalog.sample(), objectMapper, true);
		McpReadService service = new McpReadService(new StubProjectQueryGateway(), writes,
				McpRateLimiter.NOOP, objectMapper);

		List<McpToolDescriptor> tools =
				(List<McpToolDescriptor>) service.listTools().get("tools");

		assertThat(tools).isNotEmpty();
		assertThat(tools).extracting(McpToolDescriptor::name)
				.allSatisfy(name -> assertThat(name)
						.as("MCP tool name '%s' must match %s", name, MCP_TOOL_NAME.pattern())
						.matches(MCP_TOOL_NAME));
		// Sanity: both a read and a write tool are present.
		assertThat(tools).extracting(McpToolDescriptor::name)
				.contains("listProjects", "EditGoal", "runCommand");
	}
}
