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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.CommandInputSchema;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.gateway.tracker.RequirementGoalUpserter;
import com.rreganjr.requel.gateway.tracker.UpsertGoalRequest;

/**
 * MCP write tools, backed by the {@link CommandGateway}. Exposes a generic
 * {@code runCommand} (any allowlisted command type + JSON input) plus one typed tool per command
 * in the shared {@link GatewayCommandCatalog}. The catalog is the single source of truth for the
 * gateway's exposed write surface — the same set the allow/deny {@code CommandPolicy} enforces and
 * the CLI discovers via {@code /api/gateway/commands/descriptors} — so MCP, the CLI, and the policy
 * stay in lockstep (issue #104). Each typed tool is named after its command type (e.g.
 * {@code EditGoal}) and its JSON schema is derived from the command's registered input DTO, so the
 * tools can never drift from what the gateway actually permits.
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
	static final String RUN_COMMAND = "runCommand";

	/** Convenience tool: create/update a goal from a requirement + provenance (issue #71). */
	static final String UPSERT_GOAL = "upsertGoalFromRequirement";

	/**
	 * Composite convenience tools — multi-command orchestrations that are NOT single catalog
	 * commands, so the "typed write tools == catalog" lockstep checks exclude them. They are still
	 * gated by the write flag and every underlying write still goes through the gateway's policy,
	 * authorization, and audit.
	 */
	static final Set<String> COMPOSITE_TOOLS = Set.of(UPSERT_GOAL);

	private final CommandGateway commandGateway;
	private final QueryGateway queryGateway;
	private final GatewayCommandCatalog catalog;
	private final ObjectMapper objectMapper;
	private final boolean writeEnabled;

	/** Lazily built (only when the upsert tool is called), so a null command/query gateway in
	 * read-only mode never triggers construction. */
	private RequirementGoalUpserter upserter;

	@Autowired
	public McpWriteService(CommandGateway commandGateway, QueryGateway queryGateway,
			GatewayCommandCatalog catalog, ObjectMapper objectMapper,
			@Value("${requel.gateway.write.enabled:false}") boolean writeEnabled) {
		this.commandGateway = commandGateway;
		this.queryGateway = queryGateway;
		this.catalog = catalog;
		this.objectMapper = objectMapper;
		this.writeEnabled = writeEnabled;
	}

	/**
	 * Convenience constructor without a {@link QueryGateway} (used by read-only deployments and by
	 * tests that never exercise the composite {@code upsertGoalFromRequirement} tool). The upsert
	 * tool requires a query gateway and will fail fast if invoked through this path.
	 */
	public McpWriteService(CommandGateway commandGateway, GatewayCommandCatalog catalog,
			ObjectMapper objectMapper, boolean writeEnabled) {
		this(commandGateway, null, catalog, objectMapper, writeEnabled);
	}

	public boolean isWriteEnabled() {
		return writeEnabled;
	}

	/** @return true if {@code toolName} is a write tool (regardless of the opt-in flag). */
	public boolean handles(String toolName) {
		return RUN_COMMAND.equals(toolName) || COMPOSITE_TOOLS.contains(toolName)
				|| catalog.find(toolName).isPresent();
	}

	/** Write tool descriptors for {@code tools/list}; empty when the write flag is disabled. */
	public List<McpToolDescriptor> toolDescriptors() {
		if (!writeEnabled) {
			return List.of();
		}
		List<McpToolDescriptor> tools = new ArrayList<>();
		tools.add(new McpToolDescriptor(RUN_COMMAND,
				"Execute any gateway-allowlisted Requel command by type with a JSON input"
						+ " object. Subject to the gateway allow/deny policy and the caller's"
						+ " stakeholder permissions.",
				runCommandSchema()));
		// One typed tool per catalog command, generated from the shared catalog so the MCP write
		// surface is exactly the gateway's exposed write surface (issue #104).
		for (CommandDescriptor descriptor : catalog.descriptors()) {
			tools.add(new McpToolDescriptor(descriptor.commandType(), describe(descriptor),
					CommandInputSchema.of(descriptor.inputType())));
		}
		// Composite convenience tools (orchestrations over several commands; issue #71).
		tools.add(upsertGoalDescriptor());
		return tools;
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
		if (UPSERT_GOAL.equals(toolName)) {
			return upsertGoalFromRequirement(arguments);
		}
		// Typed tool: the tool name IS the catalog command type, and its argument names already
		// match the command input DTO field names, so forward the arguments as-is.
		if (catalog.find(toolName).isEmpty()) {
			throw new McpInvalidParamsException("Unknown MCP write tool: " + toolName);
		}
		return execute(toolName, arguments == null ? Map.of() : toMap(arguments));
	}

	private Object execute(String commandType, Object input) {
		try {
			GatewayResult result = commandGateway.execute(
					new GatewayRequest(commandType, input, McpClientContext.clientId()));
			return result.result() != null ? result.result()
					: Map.of("ok", true, "commandType", commandType);
		} catch (GatewayException e) {
			throw mapGatewayException(e);
		}
	}

	/**
	 * Composite tool: create or update a goal from one requirement and (re)attach its provenance
	 * note (issue #71). The per-client identity is taken from the request context, not the
	 * arguments, mirroring {@link #execute}.
	 */
	private Object upsertGoalFromRequirement(JsonNode arguments) {
		UpsertGoalRequest request;
		try {
			request = new UpsertGoalRequest(
					requiredText(arguments, "projectName"),
					requiredText(arguments, "criterionText"),
					optionalText(arguments, "name"),
					optionalText(arguments, "text"),
					requiredText(arguments, "sourceSystem"),
					requiredText(arguments, "sourceRef"),
					optionalText(arguments, "sourceUrl"),
					optionalText(arguments, "criterionRef"),
					McpClientContext.clientId(),
					optionalText(arguments, "criterionHash"));
		} catch (IllegalArgumentException e) {
			throw new McpInvalidParamsException(e.getMessage());
		}
		try {
			return upserter().upsert(request);
		} catch (GatewayException e) {
			throw mapGatewayException(e);
		}
	}

	private RequirementGoalUpserter upserter() {
		if (upserter == null) {
			upserter = new RequirementGoalUpserter(commandGateway, queryGateway);
		}
		return upserter;
	}

	private static RuntimeException mapGatewayException(GatewayException e) {
		return switch (e.getKind()) {
			// Client-correctable failures map to JSON-RPC INVALID_PARAMS.
			case NOT_ALLOWED, NOT_FOUND, INVALID_INPUT, UNAUTHORIZED ->
					new McpInvalidParamsException(e.getMessage());
			// Server-side execution failure maps to INTERNAL_ERROR.
			case EXECUTION_ERROR -> new IllegalStateException(e.getMessage(), e);
		};
	}

	// ---- schema + description helpers ----------------------------------------------------------

	/** Human-readable tool description: the catalog title/description plus the input field names. */
	private static String describe(CommandDescriptor descriptor) {
		String base = descriptor.description() != null && !descriptor.description().isBlank()
				? descriptor.description()
				: descriptor.title();
		List<String> fields = CommandInputSchema.fieldNames(descriptor.inputType());
		String fieldHint = fields.isEmpty() ? "" : " Input fields: " + String.join(", ", fields) + ".";
		return base + "." + fieldHint;
	}

	private Map<String, Object> runCommandSchema() {
		return CommandInputSchema.objectSchema(Map.of(
				"commandType", CommandInputSchema.stringType(),
				"input", Map.of("type", "object")),
				List.of("commandType"));
	}

	private static McpToolDescriptor upsertGoalDescriptor() {
		return new McpToolDescriptor(UPSERT_GOAL,
				"Create or update a project goal from one requirement / acceptance criterion and"
						+ " attach a machine-parseable provenance note linking it to the source"
						+ " tracker item. Resolves an existing goal by provenance"
						+ " (sourceSystem + sourceRef + criterionHash) and updates it in place on a"
						+ " re-run; otherwise creates a new goal (disambiguating the name on a"
						+ " collision). name, text and criterionHash are derived from criterionText"
						+ " when omitted. Subject to the caller's Goal Edit permission.",
				upsertGoalSchema());
	}

	private static Map<String, Object> upsertGoalSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("projectName", CommandInputSchema.stringType());
		properties.put("criterionText", CommandInputSchema.stringType());
		properties.put("sourceSystem", CommandInputSchema.stringType());
		properties.put("sourceRef", CommandInputSchema.stringType());
		properties.put("name", CommandInputSchema.stringType());
		properties.put("text", CommandInputSchema.stringType());
		properties.put("sourceUrl", CommandInputSchema.stringType());
		properties.put("criterionRef", CommandInputSchema.stringType());
		properties.put("criterionHash", CommandInputSchema.stringType());
		// client is intentionally omitted: it is taken from the MCP client context, not arguments.
		return CommandInputSchema.objectSchema(properties,
				List.of("projectName", "criterionText", "sourceSystem", "sourceRef"));
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

	private static String optionalText(JsonNode params, String fieldName) {
		JsonNode value = params == null ? null : params.get(fieldName);
		return value == null || value.isNull() || !value.isTextual() ? null : value.asText();
	}
}
