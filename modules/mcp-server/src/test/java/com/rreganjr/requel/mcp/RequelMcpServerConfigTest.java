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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Verifies the Spring AI {@link ToolCallbackProvider} mirrors the JSON-RPC tool surface: read
 * tools always present, write tools only when enabled, and a callback's {@code call} delegates to
 * the same gateway-backed {@link McpReadService} dispatch.
 */
class RequelMcpServerConfigTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RequelMcpServerConfig config = new RequelMcpServerConfig();

	private List<String> toolNames(ToolCallbackProvider provider) {
		return Arrays.stream(provider.getToolCallbacks())
				.map(tc -> tc.getToolDefinition().name())
				.toList();
	}

	@Test
	void exposesReadToolsAndOmitsWriteToolsWhenDisabled() {
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(readOnly, objectMapper);

		List<String> names = toolNames(provider);
		assertThat(names).contains("requel.listProjects", "requel.getProject",
				"requel.getOpenIssues", "requel.draftAnnotation");
		assertThat(names).noneMatch(n -> n.equals("requel.runCommand")
				|| n.equals("requel.createGoal"));
	}

	@Test
	void includesWriteToolsWhenEnabled() {
		McpWriteService writes = new McpWriteService(
				request -> new GatewayResult(request.commandType(), null), objectMapper, true);
		McpReadService withWrites = new McpReadService(new StubProjectQueryGateway(), writes,
				objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(withWrites, objectMapper);

		assertThat(toolNames(provider)).contains("requel.runCommand", "requel.createGoal");
	}

	@Test
	void callDelegatesToGateway() {
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(readOnly, objectMapper);
		ToolCallback getProject = Arrays.stream(provider.getToolCallbacks())
				.filter(tc -> tc.getToolDefinition().name().equals("requel.getProject"))
				.findFirst().orElseThrow();

		String result = getProject.call("{\"projectName\":\"Sample\"}");
		assertThat(result).contains("Sample");
	}
}
