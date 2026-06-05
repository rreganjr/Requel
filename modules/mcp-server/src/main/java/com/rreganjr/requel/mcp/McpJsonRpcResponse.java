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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpJsonRpcResponse(String jsonrpc, JsonNode id, Object result,
		McpJsonRpcError error) {

	public static McpJsonRpcResponse result(JsonNode id, Object result) {
		return new McpJsonRpcResponse("2.0", id, result, null);
	}

	public static McpJsonRpcResponse error(JsonNode id, int code, String message) {
		return new McpJsonRpcResponse("2.0", id, null, new McpJsonRpcError(code, message));
	}
}
