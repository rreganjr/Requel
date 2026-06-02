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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class McpReadService {

	private static final String JSON_MIME_TYPE = "application/json";

	private final ProjectQueryGateway projectQueryGateway;
	private final ObjectMapper objectMapper;

	@Autowired
	public McpReadService(ProjectQueryGateway projectQueryGateway, ObjectMapper objectMapper) {
		this.projectQueryGateway = projectQueryGateway;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> initialize() {
		return Map.of(
				"protocolVersion", "2025-03-26",
				"capabilities", Map.of("tools", Map.of(), "resources", Map.of()),
				"serverInfo", Map.of("name", "requel-mcp-server", "version", "2.0.0-dev"));
	}

	public Map<String, Object> listTools() {
		return Map.of("tools", List.of(
				new McpToolDescriptor("requel.listProjects",
						"List projects visible to the current authenticated user.",
						Map.of("type", "object", "properties", Map.of(), "additionalProperties",
								false)),
				new McpToolDescriptor("requel.getProject",
						"Read one project summary by project name.",
						projectNameSchema()),
				new McpToolDescriptor("requel.getProjectTree",
						"Read the project content tree by project name.",
						projectNameSchema()),
				new McpToolDescriptor("requel.getGlossary",
						"Read the glossary terms defined in a project.",
						projectNameSchema()),
				new McpToolDescriptor("requel.getOpenIssues",
						"List the unresolved issues across all entities in a project.",
						projectNameSchema()),
				new McpToolDescriptor("requel.getAnnotations",
						"Read the notes and issues attached to one entity (by type and id).",
						entityRefSchema()),
				new McpToolDescriptor("requel.getEntity",
						"Read one entity (Goal, Story, Actor, UseCase, Scenario, or GlossaryTerm)"
								+ " by type and id.",
						entityRefSchema()),
				new McpToolDescriptor("requel.getEntityNeighbors",
						"Read an entity's related entities, grouped by relationship.",
						entityRefSchema())));
	}

	public Map<String, Object> callTool(JsonNode params) {
		String name = requiredText(params, "name");
		JsonNode arguments = params != null ? params.get("arguments") : null;
		Object result = switch (name) {
			case "requel.listProjects" -> projectQueryGateway.listProjects();
			case "requel.getProject" -> projectQueryGateway.getProject(requiredText(arguments,
					"projectName"));
			case "requel.getProjectTree" -> projectQueryGateway.getProjectTree(requiredText(
					arguments, "projectName"));
			case "requel.getGlossary" -> projectQueryGateway.getGlossaryTerms(requiredText(
					arguments, "projectName"));
			case "requel.getOpenIssues" -> projectQueryGateway.getOpenIssues(requiredText(
					arguments, "projectName"));
			case "requel.getAnnotations" -> projectQueryGateway.getAnnotations(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "requel.getEntity" -> projectQueryGateway.getEntity(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "requel.getEntityNeighbors" -> projectQueryGateway.getEntityNeighbors(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			default -> throw new McpInvalidParamsException("Unknown MCP tool: " + name);
		};
		return Map.of("content", List.of(new McpTextContent("text", toJson(result))),
				"isError", false);
	}

	public Map<String, Object> listResources() {
		return Map.of("resources", List.of(
				new McpResourceDescriptor("requel://projects", "Visible Requel projects",
						"Projects visible to the current user", JSON_MIME_TYPE),
				new McpResourceDescriptor("requel://projects/{projectName}/glossary",
						"Project glossary", "Glossary terms defined in a project (template URI)",
						JSON_MIME_TYPE),
				new McpResourceDescriptor("requel://projects/{projectName}/open-issues",
						"Project open issues",
						"Unresolved issues across a project's entities (template URI)",
						JSON_MIME_TYPE)));
	}

	public Map<String, Object> readResource(JsonNode params) {
		String uri = requiredText(params, "uri");
		Object result = readUri(uri);
		return Map.of("contents", List.of(new McpResourceContent(uri, JSON_MIME_TYPE,
				toJson(result))));
	}

	private Object readUri(String uri) {
		if ("requel://projects".equals(uri)) {
			return projectQueryGateway.listProjects();
		}
		String prefix = "requel://projects/";
		if (!uri.startsWith(prefix)) {
			throw new McpInvalidParamsException("Unsupported MCP resource URI: " + uri);
		}
		String remainder = uri.substring(prefix.length());
		if (remainder.endsWith("/tree")) {
			return projectQueryGateway.getProjectTree(stripSuffix(remainder, "/tree"));
		}
		if (remainder.endsWith("/glossary")) {
			return projectQueryGateway.getGlossaryTerms(stripSuffix(remainder, "/glossary"));
		}
		if (remainder.endsWith("/open-issues")) {
			return projectQueryGateway.getOpenIssues(stripSuffix(remainder, "/open-issues"));
		}
		return projectQueryGateway.getProject(remainder);
	}

	private static String stripSuffix(String value, String suffix) {
		return value.substring(0, value.length() - suffix.length());
	}

	private Map<String, Object> projectNameSchema() {
		return Map.of("type", "object",
				"properties", Map.of("projectName", Map.of("type", "string")),
				"required", List.of("projectName"), "additionalProperties", false);
	}

	private Map<String, Object> entityRefSchema() {
		return Map.of("type", "object",
				"properties", Map.of(
						"projectName", Map.of("type", "string"),
						"entityType", Map.of("type", "string"),
						"entityId", Map.of("type", "integer")),
				"required", List.of("projectName", "entityType", "entityId"),
				"additionalProperties", false);
	}

	private long requiredLong(JsonNode params, String fieldName) {
		JsonNode value = params == null ? null : params.get(fieldName);
		if (value == null || !value.canConvertToLong()) {
			throw new McpInvalidParamsException("Missing required integer field: " + fieldName);
		}
		return value.asLong();
	}

	private String requiredText(JsonNode params, String fieldName) {
		if (params == null || params.get(fieldName) == null || !params.get(fieldName).isTextual()) {
			throw new McpInvalidParamsException("Missing required string field: " + fieldName);
		}
		return params.get(fieldName).asText();
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Could not serialize MCP payload", e);
		}
	}
}
