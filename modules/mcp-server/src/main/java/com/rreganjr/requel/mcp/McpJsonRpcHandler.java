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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McpJsonRpcHandler {

	private static final int METHOD_NOT_FOUND = -32601;
	private static final int INVALID_PARAMS = -32602;
	private static final int INTERNAL_ERROR = -32603;

	private final McpReadService mcpReadService;
	private final McpCallAuditor auditor;

	@Autowired
	public McpJsonRpcHandler(McpReadService mcpReadService, McpCallAuditor auditor) {
		this.mcpReadService = mcpReadService;
		this.auditor = auditor;
	}

	public McpJsonRpcResponse handle(McpJsonRpcRequest request) {
		long startNanos = System.nanoTime();
		McpJsonRpcResponse response = dispatch(request);
		auditor.record(request, response, startNanos);
		return response;
	}

	private McpJsonRpcResponse dispatch(McpJsonRpcRequest request) {
		if (request == null) {
			return McpJsonRpcResponse.error(null, INTERNAL_ERROR, "Request body is required");
		}
		try {
			Object result = switch (request.method()) {
				case "initialize" -> mcpReadService.initialize();
				case "tools/list" -> mcpReadService.listTools();
				case "tools/call" -> mcpReadService.callTool(request.params());
				case "resources/list" -> mcpReadService.listResources();
				case "resources/read" -> mcpReadService.readResource(request.params());
				default -> throw new McpMethodNotFoundException(request.method());
			};
			return McpJsonRpcResponse.result(request.id(), result);
		} catch (McpMethodNotFoundException e) {
			return McpJsonRpcResponse.error(request.id(), METHOD_NOT_FOUND, e.getMessage());
		} catch (McpInvalidParamsException e) {
			return McpJsonRpcResponse.error(request.id(), INVALID_PARAMS, e.getMessage());
		} catch (RuntimeException e) {
			return McpJsonRpcResponse.error(request.id(), INTERNAL_ERROR, e.getMessage());
		}
	}
}
