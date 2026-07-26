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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;

/**
 * Step 8 (issue #43): every MCP {@code tools/call} is audited with the triggering user
 * resolved from the security context, the tool name, and the outcome. Authorization itself
 * is handled by the existing security chain (the endpoint is mounted under {@code /api/mcp/**});
 * here we drive the live Spring AI {@link ToolCallback} transport within an authenticated
 * security context and assert the audit row, plus the error-outcome path.
 */
public class McpCallAuditIT extends AbstractIntegrationTestCase {

	private ToolCallbackProvider requelToolCallbackProvider;
	private McpCallAuditRepository mcpCallAuditRepository;

	@Autowired
	protected void setToolCallbackProvider(ToolCallbackProvider requelToolCallbackProvider) {
		this.requelToolCallbackProvider = requelToolCallbackProvider;
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
	public void successfulToolCallIsAuditedWithTriggeringUser() {
		authenticateAs("project");
		User user = getUserRepository().findUserByUsername("project");

		callTool("listProjects", "{}");

		McpCallAudit audit = latestAuditForTool("listProjects");
		assertNotNull(audit, "an audit row should be recorded for the tools/call");
		assertEquals("tools/call", audit.getMethod());
		assertEquals(user.getId(), audit.getTriggeringUserId());
		assertEquals("OK", audit.getStatus());
		assertNull(audit.getErrorCode());
		assertNotNull(audit.getCalledAt());
	}

	@Test
	public void failedToolCallIsAuditedWithErrorOutcome() {
		authenticateAs("project");

		// Missing the required 'projectName' argument: the tool throws, and the failure is audited.
		assertThrows(RuntimeException.class, () -> callTool("getProject", "{}"));

		McpCallAudit audit = latestAuditForTool("getProject");
		assertNotNull(audit, "an audit row should be recorded for the failed call");
		assertEquals("tools/call", audit.getMethod());
		assertEquals("ERROR", audit.getStatus());
		assertNotNull(audit.getErrorSummary());
	}

	/** Invoke a tool by name through the live Spring AI {@link ToolCallback} transport. */
	private String callTool(String toolName, String argumentsJson) {
		for (ToolCallback candidate : requelToolCallbackProvider.getToolCallbacks()) {
			if (candidate.getToolDefinition().name().equals(toolName)) {
				return candidate.call(argumentsJson);
			}
		}
		throw new AssertionError("MCP tool not found: " + toolName);
	}

	private McpCallAudit latestAuditForTool(String toolName) {
		return mcpCallAuditRepository.findAll().stream()
				.filter(a -> "tools/call".equals(a.getMethod()) && toolName.equals(a.getToolName()))
				.reduce((first, second) -> second)
				.orElse(null);
	}

	private static void authenticateAs(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, null, List.of()));
	}
}
