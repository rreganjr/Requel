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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Verifies the Spring AI {@link ToolCallbackProvider} exposes Requel's tool surface: read
 * tools always present, write tools only when enabled, and a callback's {@code call} delegates to
 * the same gateway-backed {@link McpReadService} dispatch.
 */
class RequelMcpServerConfigTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RequelMcpServerConfig config = new RequelMcpServerConfig();
	// Null repository/resolver: McpCallAuditor.recordToolCall is best-effort and swallows the
	// resulting NPE, so it acts as a no-op auditor in these unit tests.
	private final McpCallAuditor auditor = new McpCallAuditor(null, null);

	private List<String> toolNames(ToolCallbackProvider provider) {
		return Arrays.stream(provider.getToolCallbacks())
				.map(tc -> tc.getToolDefinition().name())
				.toList();
	}

	@Test
	void exposesReadToolsAndOmitsWriteToolsWhenDisabled() {
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(readOnly, auditor, objectMapper);

		List<String> names = toolNames(provider);
		assertThat(names).contains("listProjects", "getProject",
				"getOpenIssues", "draftAnnotation");
		assertThat(names).noneMatch(n -> n.equals("runCommand")
				|| n.equals("EditGoal"));
	}

	@Test
	void includesWriteToolsWhenEnabled() {
		McpWriteService writes = new McpWriteService(
				request -> new GatewayResult(request.commandType(), null),
				McpTestCatalog.sample(), objectMapper, true);
		McpReadService withWrites = new McpReadService(new StubProjectQueryGateway(), writes,
				McpRateLimiter.NOOP, objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(withWrites, auditor,
				objectMapper);

		assertThat(toolNames(provider)).contains("runCommand", "EditGoal");
	}

	@Test
	void toolCallIsAuditedOnTheSpringAiPath() {
		List<String> audited = new ArrayList<>();
		McpCallAuditor recording = new McpCallAuditor(null, null) {
			@Override
			public void recordToolCall(String toolName, boolean ok, Integer errorCode,
					String errorSummary, long startNanos) {
				audited.add(toolName + ":" + ok);
			}
		};
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(readOnly, recording,
				objectMapper);
		ToolCallback getProject = Arrays.stream(provider.getToolCallbacks())
				.filter(tc -> tc.getToolDefinition().name().equals("getProject"))
				.findFirst().orElseThrow();

		getProject.call("{\"projectName\":\"Sample\"}");
		assertThat(audited).contains("getProject:true");
	}

	@Test
	void callDelegatesToGateway() {
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		ToolCallbackProvider provider = config.requelToolCallbackProvider(readOnly, auditor, objectMapper);
		ToolCallback getProject = Arrays.stream(provider.getToolCallbacks())
				.filter(tc -> tc.getToolDefinition().name().equals("getProject"))
				.findFirst().orElseThrow();

		String result = getProject.call("{\"projectName\":\"Sample\"}");
		assertThat(result).contains("Sample");
	}
}
