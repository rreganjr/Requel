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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class McpJsonRpcHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Auditing is a side concern here; this auditor has null collaborators and its
	 * {@code record} swallows the resulting failures, so it is a safe no-op for these
	 * dispatch-focused unit tests. Audit persistence is covered by McpCallAuditIT.
	 */
	private static McpCallAuditor noOpAuditor() {
		return new McpCallAuditor(null, null);
	}

	@Test
	void handlesToolsListRequest() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper), noOpAuditor());

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree(1), "tools/list", null));

		assertThat(response.error()).isNull();
		JsonNode result = objectMapper.valueToTree(response.result());
		assertThat(result.path("tools")).hasSize(11);
		assertThat(result.path("tools").get(0).path("name").asText()).isEqualTo(
				"requel.listProjects");
	}

	@Test
	void returnsMethodNotFoundForUnknownMethod() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper), noOpAuditor());

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree("abc"), "prompts/list", null));

		assertThat(response.result()).isNull();
		assertThat(response.error().code()).isEqualTo(-32601);
		assertThat(response.error().message()).contains("prompts/list");
	}

	@Test
	void returnsInvalidParamsForUnknownTool() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper), noOpAuditor());
		JsonNode params = objectMapper.createObjectNode().put("name", "requel.bogusTool");

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree(2), "tools/call", params));

		assertThat(response.result()).isNull();
		assertThat(response.error().code()).isEqualTo(-32602);
	}

	@Test
	void returnsInvalidParamsForMissingRequiredArgument() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper), noOpAuditor());
		// requel.getProject requires arguments.projectName, which is absent here.
		JsonNode params = objectMapper.createObjectNode().put("name", "requel.getProject");

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree(3), "tools/call", params));

		assertThat(response.result()).isNull();
		assertThat(response.error().code()).isEqualTo(-32602);
	}
}
