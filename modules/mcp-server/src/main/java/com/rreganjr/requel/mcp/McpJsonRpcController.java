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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON-RPC endpoint for the in-process MCP read server. It is mounted under
 * {@code /api/**}, so the existing JWT security chain and current-user
 * resolution are reused.
 */
@RestController
@RequestMapping("/api/mcp")
public class McpJsonRpcController {

	private final McpJsonRpcHandler handler;

	@Autowired
	public McpJsonRpcController(McpJsonRpcHandler handler) {
		this.handler = handler;
	}

	@PostMapping
	public ResponseEntity<McpJsonRpcResponse> handle(@RequestBody McpJsonRpcRequest request) {
		return ResponseEntity.ok(handler.handle(request));
	}
}
