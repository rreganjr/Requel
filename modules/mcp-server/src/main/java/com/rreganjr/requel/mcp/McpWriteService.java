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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;

/**
 * MCP write tools, backed by the {@link CommandGateway}. Exposes a generic
 * {@code requel.runCommand} (any allowlisted command type + JSON input) plus a curated set of
 * ergonomic typed tools whose argument names already match the command input DTOs, so they are
 * thin wrappers that fix the command type and forward the arguments.
 * <p>
 * Writes are <strong>opt-in</strong> via {@code requel.gateway.write.enabled} (default
 * {@code false}). When disabled, the write tools are omitted from {@code tools/list} and any direct
 * call is rejected with a clear message — reads are unaffected. The gateway still enforces its
 * allow/deny policy and per-stakeholder authorization for every write, and the command chain still
 * audits it; this flag is the coarse on/off switch for the whole write surface.
 */
@Service
public class McpWriteService {

	/** Generic escape hatch: run any allowlisted command by type + input. */
	static final String RUN_COMMAND = "requel.runCommand";

	/** Typed convenience tools -> the gateway command type each forwards to. */
	private static final Map<String, String> TYPED_TOOLS = Map.of(
			"requel.createProject", "EditProject",
			"requel.createGoal", "EditGoal",
			"requel.editGoal", "EditGoal",
			"requel.addGoalToContainer", "AddGoalToGoalContainer",
			"requel.createNote", "EditNote",
			"requel.createIssue", "EditIssue");

	private final CommandGateway commandGateway;
	private final ObjectMapper objectMapper;
	private final boolean writeEnabled;

	public McpWriteService(CommandGateway commandGateway, ObjectMapper objectMapper,
			@Value("${requel.gateway.write.enabled:false}") boolean writeEnabled) {
		this.commandGateway = commandGateway;
		this.objectMapper = objectMapper;
		this.writeEnabled = writeEnabled;
	}

	public boolean isWriteEnabled() {
		return writeEnabled;
	}

	/** @return true if {@code toolName} is a write tool (regardless of the opt-in flag). */
	public boolean handles(String toolName) {
		return RUN_COMMAND.equals(toolName) || TYPED_TOOLS.containsKey(toolName);
	}

	/** Write tool descriptors for {@code tools/list}; empty when the write flag is disabled. */
	public List<McpToolDescriptor> toolDescriptors() {
		if (!writeEnabled) {
			return List.of();
		}
		return List.of(
				new McpToolDescriptor(RUN_COMMAND,
						"Execute any gateway-allowlisted Requel command by type with a JSON input"
								+ " object. Subject to the gateway allow/deny policy and the caller's"
								+ " stakeholder permissions.",
						runCommandSchema()),
				new McpToolDescriptor("requel.createProject",
						"Create a project. Arguments: name, organizationName, optional description.",
						objectSchema(Map.of("name", stringType(), "organizationName", stringType(),
								"description", stringType()), List.of("name", "organizationName"))),
				new McpToolDescriptor("requel.createGoal",
						"Create a goal in a project. Arguments: projectName, name, optional text.",
						objectSchema(Map.of("projectName", stringType(), "name", stringType(),
								"text", stringType()), List.of("projectName", "name"))),
				new McpToolDescriptor("requel.editGoal",
						"Edit an existing goal. Arguments: projectName, goalId, optional name/text.",
						objectSchema(Map.of("projectName", stringType(), "goalId", integerType(),
								"name", stringType(), "text", stringType()),
								List.of("projectName", "goalId"))),
				new McpToolDescriptor("requel.addGoalToContainer",
						"Associate a goal with a container (Project, Story, UseCase, Actor, or"
								+ " Stakeholder). Arguments: projectName, goalId, goalContainerId,"
								+ " containerType.",
						objectSchema(Map.of("projectName", stringType(), "goalId", integerType(),
								"goalContainerId", integerType(), "containerType", stringType()),
								List.of("projectName", "goalId", "goalContainerId", "containerType"))),
				new McpToolDescriptor("requel.createNote",
						"Attach a note to an entity. Arguments: projectName, entityType, entityId,"
								+ " text.",
						objectSchema(Map.of("projectName", stringType(), "entityType", stringType(),
								"entityId", integerType(), "text", stringType()),
								List.of("projectName", "entityType", "entityId", "text"))),
				new McpToolDescriptor("requel.createIssue",
						"Raise an issue on an entity. Arguments: projectName, entityType, entityId,"
								+ " text, optional mustBeResolved.",
						objectSchema(Map.of("projectName", stringType(), "entityType", stringType(),
								"entityId", integerType(), "text", stringType(),
								"mustBeResolved", booleanType()),
								List.of("projectName", "entityType", "entityId", "text"))));
	}

	/**
	 * Execute a write tool. The result payload (a DTO, or {@code {ok:true}} for commands that
	 * return nothing) is returned to {@link McpReadService} for content wrapping.
	 */
	public Object call(String toolName, JsonNode arguments) {
		if (!writeEnabled) {
			throw new McpInvalidParamsException(
					"Write tools are disabled; set requel.gateway.write.enabled=true to enable them");
		}
		if (RUN_COMMAND.equals(toolName)) {
			String commandType = requiredText(arguments, "commandType");
			JsonNode input = arguments == null ? null : arguments.get("input");
			return execute(commandType, input == null || input.isNull() ? Map.of() : toMap(input));
		}
		String commandType = TYPED_TOOLS.get(toolName);
		if (commandType == null) {
			throw new McpInvalidParamsException("Unknown MCP write tool: " + toolName);
		}
		// Typed-tool arguments already match the command input DTO field names; forward as-is.
		return execute(commandType, arguments == null ? Map.of() : toMap(arguments));
	}

	private Object execute(String commandType, Object input) {
		try {
			GatewayResult result = commandGateway.execute(
					new GatewayRequest(commandType, input, McpClientContext.clientId()));
			return result.result() != null ? result.result()
					: Map.of("ok", true, "commandType", commandType);
		} catch (GatewayException e) {
			throw switch (e.getKind()) {
				// Client-correctable failures map to JSON-RPC INVALID_PARAMS.
				case NOT_ALLOWED, NOT_FOUND, INVALID_INPUT, UNAUTHORIZED ->
						new McpInvalidParamsException(e.getMessage());
				// Server-side execution failure maps to INTERNAL_ERROR.
				case EXECUTION_ERROR -> new IllegalStateException(e.getMessage(), e);
			};
		}
	}

	// ---- schema + json helpers -----------------------------------------------------------------

	private Map<String, Object> runCommandSchema() {
		return objectSchema(Map.of(
				"commandType", stringType(),
				"input", Map.of("type", "object")),
				List.of("commandType"));
	}

	private static Map<String, Object> objectSchema(Map<String, Object> properties,
			List<String> required) {
		return Map.of("type", "object", "properties", properties, "required", required,
				"additionalProperties", false);
	}

	private static Map<String, Object> stringType() {
		return Map.of("type", "string");
	}

	private static Map<String, Object> integerType() {
		return Map.of("type", "integer");
	}

	private static Map<String, Object> booleanType() {
		return Map.of("type", "boolean");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> toMap(JsonNode node) {
		return objectMapper.convertValue(node, Map.class);
	}

	private static String requiredText(JsonNode params, String fieldName) {
		if (params == null || params.get(fieldName) == null || !params.get(fieldName).isTextual()) {
			throw new McpInvalidParamsException("Missing required string field: " + fieldName);
		}
		return params.get(fieldName).asText();
	}
}
