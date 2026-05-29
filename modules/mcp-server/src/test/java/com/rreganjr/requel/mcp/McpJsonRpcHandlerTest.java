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

	@Test
	void handlesToolsListRequest() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper));

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree(1), "tools/list", null));

		assertThat(response.error()).isNull();
		JsonNode result = objectMapper.valueToTree(response.result());
		assertThat(result.path("tools")).hasSize(3);
		assertThat(result.path("tools").get(0).path("name").asText()).isEqualTo(
				"requel.listProjects");
	}

	@Test
	void returnsMethodNotFoundForUnknownMethod() {
		McpJsonRpcHandler handler = new McpJsonRpcHandler(new McpReadService(
				new StubProjectQueryGateway(), objectMapper));

		McpJsonRpcResponse response = handler.handle(new McpJsonRpcRequest("2.0",
				objectMapper.valueToTree("abc"), "prompts/list", null));

		assertThat(response.result()).isNull();
		assertThat(response.error().code()).isEqualTo(-32601);
		assertThat(response.error().message()).contains("prompts/list");
	}
}
