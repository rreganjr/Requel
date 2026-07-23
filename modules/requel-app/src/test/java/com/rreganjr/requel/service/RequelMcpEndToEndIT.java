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
package com.rreganjr.requel.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.mcp.McpCallAuditRepository;
import com.rreganjr.requel.mcp.McpJsonRpcHandler;
import com.rreganjr.requel.mcp.McpJsonRpcRequest;
import com.rreganjr.requel.mcp.McpJsonRpcResponse;
import com.rreganjr.requel.mcp.McpTextContent;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.audit.CommandAuditLog;
import com.rreganjr.requel.service.audit.CommandAuditLogRepository;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

/**
 * End-to-end smoke test for the issue #69 MCP command gateway (the Slice 7 capstone). Drives the
 * MCP server the way a client does — {@code tools/call} through {@link McpJsonRpcHandler}, as an
 * authenticated stakeholder — across a representative create/associate/annotate sequence, then
 * asserts the entities exist and that BOTH audit surfaces are populated: the command-audit rows
 * (from the command chain) and the MCP-call-audit rows (from the MCP transport).
 *
 * <p>Write tools are enabled here explicitly; the application default is now {@code true} but the
 * property is pinned so the test is independent of that.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "requel.gateway.write.enabled=true")
public class RequelMcpEndToEndIT extends AbstractIntegrationTestCase {

	@Autowired
	private McpJsonRpcHandler handler;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CommandAuditLogRepository commandAuditLogRepository;

	@Autowired
	private McpCallAuditRepository mcpCallAuditRepository;

	private String projectName;
	private String username;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");

		long ts = System.currentTimeMillis();
		projectName = "e2e-" + ts;
		EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
		projectCmd.setEditedBy(admin);
		projectCmd.setName(projectName);
		projectCmd.setText("end-to-end MCP test project");
		projectCmd.setOrganizationName("E2EOrg-" + ts);
		projectCmd = getCommandHandler().execute(projectCmd);
		Project project = projectCmd.getProject();

		username = "e2e-user-" + ts;
		EditUserCommand userCmd = getUserCommandFactory().newEditUserCommand();
		userCmd.setEditedBy(admin);
		userCmd.setUsername(username);
		userCmd.setPassword("pw");
		userCmd.setRepassword("pw");
		userCmd.setName(username);
		userCmd.setEmailAddress(username + "@example.com");
		userCmd.setPhoneNumber("");
		userCmd.setOrganizationName("E2EOrg");
		userCmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(userCmd);

		Set<String> editPerms = Arrays.stream(new Class<?>[] {Project.class, Goal.class, Actor.class,
						Story.class, UseCase.class, Scenario.class, GlossaryTerm.class,
						Stakeholder.class, ReportGenerator.class, Annotation.class})
				.map(c -> StakeholderPermissionImpl.generatePermissionKey(c,
						StakeholderPermissionType.Edit))
				.collect(Collectors.toSet());
		EditUserStakeholderCommand stakeCmd = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		stakeCmd.setEditedBy(admin);
		stakeCmd.setProjectOrDomain(project);
		stakeCmd.setUsername(username);
		stakeCmd.setStakeholderPermissions(editPerms);
		getCommandHandler().execute(stakeCmd);
	}

	@Test
	void createsEntitiesThroughMcpAndRecordsBothAuditSurfaces() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "x", List.of()));
		long mcpCallsBefore = mcpCallAuditRepository.count();
		try {
			// 1. Create a goal (EditGoal creates when no id is supplied).
			JsonNode goal = callTool("EditGoal",
					Map.of("projectName", projectName, "name", "E2E Goal", "text", "via mcp"));
			long goalId = goal.get("id").asLong();
			assertThat(goalId).isPositive();

			// 2. Create a non-user stakeholder via the generic runCommand tool.
			JsonNode stakeholder = callTool("runCommand",
					Map.of("commandType", "EditNonUserStakeholder",
							"input", Map.of("projectName", projectName, "name", "E2E Vendor")));
			long stakeholderId = stakeholder.get("id").asLong();
			assertThat(stakeholderId).isPositive();

			// 3. Associate the goal with the stakeholder (a goal container).
			callTool("AddGoalToGoalContainer",
					Map.of("projectName", projectName, "goalId", goalId,
							"goalContainerId", stakeholderId, "containerType", "NonUserStakeholder"));

			// 4. Attach a note to the goal.
			callTool("EditNote",
					Map.of("projectName", projectName, "entityType", "Goal", "entityId", goalId,
							"text", "a note added over MCP"));

			// 5. Read the project context back and confirm the goal is present.
			JsonNode context = callTool("getProjectContext",
					Map.of("projectName", projectName));
			assertThat(context.toString()).contains("E2E Goal");
		} finally {
			SecurityContextHolder.clearContext();
		}

		// Both audit surfaces populated.
		List<CommandAuditLog> commandAudits = commandAuditLogRepository.findAll();
		Set<String> auditedCommands = commandAudits.stream()
				.map(CommandAuditLog::getCommandType).collect(Collectors.toSet());
		assertThat(auditedCommands).contains("EditGoal", "EditNonUserStakeholder",
				"AddGoalToGoalContainer", "EditNote");

		long mcpCallsAfter = mcpCallAuditRepository.count();
		assertThat(mcpCallsAfter - mcpCallsBefore).isGreaterThanOrEqualTo(5);
	}

	/** Invoke a tool through the JSON-RPC handler and return the parsed result payload. */
	private JsonNode callTool(String toolName, Map<String, Object> arguments) throws Exception {
		ObjectNode params = objectMapper.createObjectNode();
		params.put("name", toolName);
		params.set("arguments", objectMapper.valueToTree(arguments));
		McpJsonRpcRequest request = new McpJsonRpcRequest("2.0", objectMapper.valueToTree(1),
				"tools/call", params);
		McpJsonRpcResponse response = handler.handle(request);
		assertThat(response.error())
				.withFailMessage("tool %s failed: %s", toolName,
						response.error() == null ? "" : response.error().message())
				.isNull();
		Map<?, ?> result = (Map<?, ?>) response.result();
		List<?> content = (List<?>) result.get("content");
		McpTextContent text = (McpTextContent) content.get(0);
		return objectMapper.readTree(text.text());
	}
}
