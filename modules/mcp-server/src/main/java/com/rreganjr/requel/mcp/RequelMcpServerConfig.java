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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registers Requel's MCP tools with the Spring AI MCP server (issue #69 Slice 4b). The Spring AI
 * MCP server boot starter auto-detects {@link ToolCallbackProvider} beans and serves them over its
 * configured transports (SSE / Streamable HTTP). We build the provider from the same
 * {@link McpReadService#listTools()} surface the hand-rolled JSON-RPC server exposes, so both
 * transports present identical tools and one implementation sits behind them.
 * <p>
 * Write tools appear only when {@code requel.gateway.write.enabled=true} (the descriptor list from
 * {@link McpReadService#listTools()} already reflects the flag). Per-stakeholder authorization and
 * the gateway allow/deny policy are enforced inside the gateways regardless of transport;
 * authentication is handled by the existing Spring Security/JWT chain because the MCP endpoints are
 * mounted under {@code /api/mcp/*}.
 */
@Configuration
public class RequelMcpServerConfig {

	@Bean
	public ToolCallbackProvider requelToolCallbackProvider(McpReadService toolService,
			ObjectMapper objectMapper) {
		List<ToolCallback> callbacks = new ArrayList<>();
		Object tools = toolService.listTools().get("tools");
		if (tools instanceof List<?> descriptors) {
			for (Object descriptor : descriptors) {
				if (descriptor instanceof McpToolDescriptor toolDescriptor) {
					callbacks.add(new RequelMcpToolCallback(toolDescriptor, toolService,
							objectMapper));
				}
			}
		}
		return ToolCallbackProvider.from(callbacks);
	}
}
