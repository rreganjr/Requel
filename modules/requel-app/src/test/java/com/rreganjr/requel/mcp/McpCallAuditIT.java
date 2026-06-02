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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;

/**
 * Step 8 (issue #43): every MCP JSON-RPC call is audited with the triggering user
 * resolved from the security context, the method, and the outcome. Authorization itself
 * is handled by the existing JWT chain (the endpoint is mounted under {@code /api/**});
 * here we drive the handler within an authenticated security context and assert the
 * audit row, plus the error-outcome path.
 */
public class McpCallAuditIT extends AbstractIntegrationTestCase {

	private McpJsonRpcHandler mcpHandler;
	private McpCallAuditRepository mcpCallAuditRepository;

	@Autowired
	protected void setMcpHandler(McpJsonRpcHandler mcpHandler) {
		this.mcpHandler = mcpHandler;
	}

	@Autowired
	protected void setMcpCallAuditRepository(McpCallAuditRepository mcpCallAuditRepository) {
		this.mcpCallAuditRepository = mcpCallAuditRepository;
	}

	@AfterEach
	public void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void successfulCallIsAuditedWithTriggeringUser() {
		authenticateAs("project");
		User user = getUserRepository().findUserByUsername("project");

		McpJsonRpcResponse response = mcpHandler
				.handle(new McpJsonRpcRequest("2.0", null, "tools/list", null));

		assertNull(response.error(), "tools/list should succeed");
		McpCallAudit audit = mcpCallAuditRepository.findAll().stream()
				.filter(a -> "tools/list".equals(a.getMethod())
						&& user.getId().equals(a.getTriggeringUserId()))
				.reduce((first, second) -> second)
				.orElse(null);
		assertNotNull(audit, "an audit row should be recorded for the tools/list call");
		assertEquals("OK", audit.getStatus());
		assertNull(audit.getErrorCode());
		assertNotNull(audit.getCalledAt());
	}

	@Test
	public void failedCallIsAuditedWithErrorCode() {
		authenticateAs("project");

		McpJsonRpcResponse response = mcpHandler
				.handle(new McpJsonRpcRequest("2.0", null, "prompts/list", null));

		assertNotNull(response.error());
		assertEquals(-32601, response.error().code());
		McpCallAudit audit = mcpCallAuditRepository.findAll().stream()
				.filter(a -> "prompts/list".equals(a.getMethod()))
				.reduce((first, second) -> second)
				.orElse(null);
		assertNotNull(audit, "an audit row should be recorded for the failed call");
		assertEquals("ERROR", audit.getStatus());
		assertEquals(Integer.valueOf(-32601), audit.getErrorCode());
	}

	private static void authenticateAs(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, null, List.of()));
	}
}
