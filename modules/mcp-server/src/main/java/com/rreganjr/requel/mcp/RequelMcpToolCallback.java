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

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Adapts one Requel MCP tool (a {@link McpToolDescriptor}) to the Spring AI {@link ToolCallback}
 * SPI, so the Spring AI MCP server transport (SSE / Streamable HTTP) exposes the very same tools
 * the hand-rolled JSON-RPC server does. Execution is delegated to {@link McpReadService#callTool}
 * — which already routes reads to the {@code QueryGateway} and writes to the {@code CommandGateway}
 * (with the write opt-in flag and per-stakeholder authorization), so there is one tool
 * implementation behind both transports.
 */
final class RequelMcpToolCallback implements ToolCallback {

	private final String toolName;
	private final ToolDefinition toolDefinition;
	private final McpReadService toolService;
	private final McpCallAuditor auditor;
	private final ObjectMapper objectMapper;

	RequelMcpToolCallback(McpToolDescriptor descriptor, McpReadService toolService,
			McpCallAuditor auditor, ObjectMapper objectMapper) {
		this.toolName = descriptor.name();
		this.toolService = toolService;
		this.auditor = auditor;
		this.objectMapper = objectMapper;
		this.toolDefinition = ToolDefinition.builder()
				.name(descriptor.name())
				.description(descriptor.description())
				.inputSchema(toJson(descriptor.inputSchema()))
				.build();
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return toolDefinition;
	}

	@Override
	public String call(String toolInput) {
		// The JSON-RPC transport audits via McpJsonRpcHandler; the Spring AI transport doesn't go
		// through it, so record the MCP-call audit row here (issue #69 Slice 5).
		long startNanos = System.nanoTime();
		JsonNode arguments = parseArguments(toolInput);
		ObjectNode params = objectMapper.createObjectNode();
		params.put("name", toolName);
		params.set("arguments", arguments);
		try {
			Map<String, Object> result = toolService.callTool(params);
			auditor.recordToolCall(toolName, true, null, null, startNanos);
			return extractText(result);
		} catch (RuntimeException e) {
			auditor.recordToolCall(toolName, false, null, e.getMessage(), startNanos);
			throw e;
		}
	}

	private JsonNode parseArguments(String toolInput) {
		if (toolInput == null || toolInput.isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(toolInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Invalid JSON arguments for tool '" + toolName + "': " + e.getMessage(), e);
		}
	}

	/**
	 * The tool result payload is the text of the single content block produced by
	 * {@link McpReadService#callTool} (already a JSON string). Fall back to serializing the whole
	 * result map if the expected shape is absent.
	 */
	private String extractText(Map<String, Object> result) {
		Object content = result.get("content");
		if (content instanceof List<?> blocks && !blocks.isEmpty()
				&& blocks.get(0) instanceof McpTextContent text) {
			return text.text();
		}
		return toJson(result);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Could not serialize MCP tool payload", e);
		}
	}
}
