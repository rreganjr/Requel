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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.QueryGateway;

@Service
public class McpReadService {

	private final QueryGateway projectQueryGateway;
	private final McpWriteService writeService;
	private final McpRateLimiter rateLimiter;
	private final ObjectMapper objectMapper;

	@Autowired
	public McpReadService(QueryGateway projectQueryGateway, McpWriteService writeService,
			McpRateLimiter rateLimiter, ObjectMapper objectMapper) {
		this.projectQueryGateway = projectQueryGateway;
		this.writeService = writeService;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Convenience constructor for read-only deployments and tests: no write tools are exposed
	 * (a disabled {@link McpWriteService} with no command gateway and an empty catalog) and the
	 * no-op rate limiter.
	 */
	public McpReadService(QueryGateway projectQueryGateway, ObjectMapper objectMapper) {
		this(projectQueryGateway,
				new McpWriteService(null, GatewayCommandCatalog.empty(), objectMapper, false),
				McpRateLimiter.NOOP, objectMapper);
	}

	public Map<String, Object> listTools() {
		List<McpToolDescriptor> tools = new ArrayList<>(List.of(
				new McpToolDescriptor("listProjects", QueryDescriptions.LIST_PROJECTS,
						Map.of("type", "object", "properties", Map.of(), "additionalProperties",
								false)),
				new McpToolDescriptor("getProject", QueryDescriptions.GET_PROJECT,
						projectNameSchema()),
				new McpToolDescriptor("getProjectTree", QueryDescriptions.GET_PROJECT_TREE,
						projectNameSchema()),
				new McpToolDescriptor("getGlossary", QueryDescriptions.GET_GLOSSARY,
						projectNameSchema()),
				new McpToolDescriptor("getOpenIssues", QueryDescriptions.GET_OPEN_ISSUES,
						projectNameSchema()),
				new McpToolDescriptor("getAnnotations", QueryDescriptions.GET_ANNOTATIONS,
						entityRefSchema(QueryDescriptions.ANNOTATABLE_ENTITY_TYPES,
								QueryDescriptions.ANNOTATABLE_ENTITY_TYPE)),
				new McpToolDescriptor("getEntity", QueryDescriptions.GET_ENTITY,
						entityRefSchema(QueryDescriptions.READABLE_ENTITY_TYPES,
								QueryDescriptions.READABLE_ENTITY_TYPE)),
				new McpToolDescriptor("getEntityNeighbors", QueryDescriptions.GET_ENTITY_NEIGHBORS,
						entityRefSchema(QueryDescriptions.READABLE_ENTITY_TYPES,
								QueryDescriptions.READABLE_ENTITY_TYPE)),
				new McpToolDescriptor("searchProjectEntities",
						QueryDescriptions.SEARCH_PROJECT_ENTITIES, searchSchema()),
				new McpToolDescriptor("getProjectContext", QueryDescriptions.GET_PROJECT_CONTEXT,
						projectNameSchema()),
				new McpToolDescriptor("getProjectContent", QueryDescriptions.GET_PROJECT_CONTENT,
						projectContentSchema()),
				new McpToolDescriptor("draftAnnotation", QueryDescriptions.DRAFT_ANNOTATION,
						draftAnnotationSchema()),
				new McpToolDescriptor("getSource", QueryDescriptions.GET_SOURCE,
						sourceSchema(false)),
				new McpToolDescriptor("findEntitiesBySource",
						QueryDescriptions.FIND_ENTITIES_BY_SOURCE, sourceSchema(true)),
				new McpToolDescriptor("getEntitySources", QueryDescriptions.GET_ENTITY_SOURCES,
						entityRefSchema(QueryDescriptions.PROVENANCE_ENTITY_TYPES,
								QueryDescriptions.PROVENANCE_ENTITY_TYPE)),
				new McpToolDescriptor("listSources", QueryDescriptions.LIST_SOURCES,
						projectNameSchema()),
				new McpToolDescriptor("compareSources", QueryDescriptions.COMPARE_SOURCES,
						compareSourcesSchema())));
		// Append opt-in write tools (empty unless requel.gateway.write.enabled=true).
		tools.addAll(writeService.toolDescriptors());
		return Map.of("tools", tools);
	}

	public Map<String, Object> callTool(JsonNode params) {
		String name = requiredText(params, "name");
		JsonNode arguments = params != null ? params.get("arguments") : null;
		// Single rate-limit chokepoint for every transport (JSON-RPC + Spring AI both route here).
		rateLimiter.check(McpClientContext.clientId(), name);
		if (writeService.handles(name)) {
			Object writeResult = writeService.call(name, arguments);
			return Map.of("content", List.of(new McpTextContent("text", toJson(writeResult))),
					"isError", false);
		}
		Object result = switch (name) {
			case "listProjects" -> projectQueryGateway.listProjects();
			case "getProject" -> projectQueryGateway.getProject(requiredText(arguments,
					"projectName"));
			case "getProjectTree" -> projectQueryGateway.getProjectTree(requiredText(
					arguments, "projectName"));
			case "getGlossary" -> projectQueryGateway.getGlossaryTerms(requiredText(
					arguments, "projectName"));
			case "getOpenIssues" -> projectQueryGateway.getOpenIssues(requiredText(
					arguments, "projectName"));
			case "getAnnotations" -> projectQueryGateway.getAnnotations(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "getEntity" -> projectQueryGateway.getEntity(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "getEntityNeighbors" -> projectQueryGateway.getEntityNeighbors(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "searchProjectEntities" -> projectQueryGateway.searchProjectEntities(
					requiredText(arguments, "projectName"), requiredText(arguments, "query"));
			case "getProjectContext" -> projectQueryGateway.getProjectContext(
					requiredText(arguments, "projectName"));
			case "getProjectContent" -> projectQueryGateway.getProjectContent(
					requiredText(arguments, "projectName"), optionalText(arguments, "annotations"));
			case "draftAnnotation" -> draftAnnotation(arguments);
			case "getSource" -> projectQueryGateway.getSource(
					requiredText(arguments, "projectName"), requiredText(arguments, "system"),
					requiredText(arguments, "externalId"));
			case "findEntitiesBySource" -> projectQueryGateway.findEntitiesBySource(
					requiredText(arguments, "projectName"), requiredText(arguments, "system"),
					requiredText(arguments, "externalId"), optionalText(arguments, "fragment"));
			case "getEntitySources" -> projectQueryGateway.getEntitySources(
					requiredText(arguments, "projectName"), requiredText(arguments, "entityType"),
					requiredLong(arguments, "entityId"));
			case "listSources" -> projectQueryGateway.listSources(
					requiredText(arguments, "projectName"));
			case "compareSources" -> projectQueryGateway.compareSources(
					requiredText(arguments, "projectName"), requiredText(arguments, "system"),
					requiredText(arguments, "externalId"), requiredText(arguments, "otherSystem"),
					requiredText(arguments, "otherExternalId"));
			default -> throw new McpInvalidParamsException("Unknown MCP tool: " + name);
		};
		return Map.of("content", List.of(new McpTextContent("text", toJson(result))),
				"isError", false);
	}

	// ---- schemas: every property carries a description (issue #296, McpReadServiceTest) ----

	private static Map<String, Object> property(String type, String description) {
		return Map.of("type", type, "description", description);
	}

	private static Map<String, Object> enumProperty(List<String> values, String description) {
		return Map.of("type", "string", "enum", values, "description", description);
	}

	private Map<String, Object> projectNameSchema() {
		return Map.of("type", "object",
				"properties", Map.of("projectName",
						property("string", QueryDescriptions.PROJECT_NAME)),
				"required", List.of("projectName"), "additionalProperties", false);
	}

	/** #274: the project name plus the optional annotation mode. */
	private Map<String, Object> projectContentSchema() {
		return Map.of("type", "object",
				"properties", Map.of(
						"projectName", property("string", QueryDescriptions.PROJECT_NAME),
						"annotations", enumProperty(QueryDescriptions.CONTENT_ANNOTATION_MODES,
								QueryDescriptions.CONTENT_ANNOTATIONS)),
				"required", List.of("projectName"), "additionalProperties", false);
	}

	/**
	 * An entity reference within a project. {@code entityTypes} differs by tool: the annotation
	 * reads accept every annotatable type, while {@code getEntity} and {@code getEntityNeighbors}
	 * accept only the types they have a detail read for.
	 */
	private Map<String, Object> entityRefSchema(List<String> entityTypes, String typeDescription) {
		return Map.of("type", "object",
				"properties", Map.of(
						"projectName", property("string", QueryDescriptions.PROJECT_NAME),
						"entityType", enumProperty(entityTypes, typeDescription),
						"entityId", property("integer", QueryDescriptions.ENTITY_ID)),
				"required", List.of("projectName", "entityType", "entityId"),
				"additionalProperties", false);
	}

	/**
	 * Build an {@link AnnotationAction} draft from the tool arguments. Read-only: the draft is
	 * returned to the caller, never persisted; the caller saves it with {@code EditNote} or
	 * {@code EditIssue}, as {@link QueryDescriptions#DRAFT_ANNOTATION} tells it.
	 */
	private AnnotationAction draftAnnotation(JsonNode arguments) {
		String entityType = requiredText(arguments, "entityType");
		long entityId = requiredLong(arguments, "entityId");
		String kind = requiredText(arguments, "kind");
		String text = requiredText(arguments, "text");
		AnnotationAction.ActionType actionType = switch (kind.toUpperCase(Locale.ROOT)) {
			case "NOTE" -> AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE;
			case "ISSUE" -> AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE;
			default -> throw new McpInvalidParamsException(
					"Unsupported annotation kind: " + kind + " (expected NOTE or ISSUE)");
		};
		EntityRef targetRef = EntityRef.of(entityType, entityId);
		String severity = severity(optionalText(arguments, "severity"));
		Map<String, Object> metadata = actionType == AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE
				// #296: false when absent, as EditIssue defaults mustBeResolved, so saving a draft
				// as-is gives the issue the draft describes.
				? Map.<String, Object>of("mustResolve", optionalBoolean(arguments, "mustResolve", false))
				: Map.of();
		String actionKey = "mcp-draft:" + entityType + ":" + entityId + ":"
				+ kind.toLowerCase(Locale.ROOT) + ":" + Integer.toHexString(text.hashCode());
		return new AnnotationAction(actionKey, actionType, targetRef, null, text, severity, null,
				List.of(), metadata);
	}

	/** The vocabulary {@code draftAnnotation} advertises and accepts (#271). */
	private static final List<String> SEVERITIES = Arrays.stream(IssueSeverity.values())
			.map(Enum::name).toList();

	/**
	 * #271: a supplied severity must be in the vocabulary. It is normalised to its upper-case name,
	 * so the draft carries exactly what {@code EditIssue} stores. Absent stays null, and the issue
	 * gets its kind's default when persisted.
	 */
	private static String severity(String value) {
		if (value == null) {
			return null;
		}
		return IssueSeverity.parse(value).map(Enum::name)
				.orElseThrow(() -> new McpInvalidParamsException("Unsupported severity: " + value
						+ " (expected LOW, MEDIUM or HIGH)"));
	}

	private Map<String, Object> draftAnnotationSchema() {
		return Map.of("type", "object",
				"properties", Map.of(
						"entityType", enumProperty(QueryDescriptions.ANNOTATABLE_ENTITY_TYPES,
								QueryDescriptions.ANNOTATABLE_ENTITY_TYPE),
						"entityId", property("integer", QueryDescriptions.ENTITY_ID),
						"kind", enumProperty(List.of("NOTE", "ISSUE"),
								QueryDescriptions.ANNOTATION_KIND),
						"text", property("string", QueryDescriptions.ANNOTATION_TEXT),
						"severity", enumProperty(SEVERITIES, QueryDescriptions.ANNOTATION_SEVERITY),
						"mustResolve", property("boolean",
								QueryDescriptions.ANNOTATION_MUST_RESOLVE)),
				"required", List.of("entityType", "entityId", "kind", "text"),
				"additionalProperties", false);
	}

	/** A source by system and external id, optionally narrowed to one fragment (#272). */
	private Map<String, Object> sourceSchema(boolean withFragment) {
		Map<String, Object> properties = new java.util.LinkedHashMap<>();
		properties.put("projectName", property("string", QueryDescriptions.PROJECT_NAME));
		properties.put("system", property("string", QueryDescriptions.SOURCE_SYSTEM));
		properties.put("externalId", property("string", QueryDescriptions.SOURCE_EXTERNAL_ID));
		if (withFragment) {
			properties.put("fragment", property("string", QueryDescriptions.SOURCE_FRAGMENT));
		}
		return Map.of("type", "object", "properties", properties,
				"required", List.of("projectName", "system", "externalId"),
				"additionalProperties", false);
	}

	private Map<String, Object> compareSourcesSchema() {
		Map<String, Object> properties = new java.util.LinkedHashMap<>();
		properties.put("projectName", property("string", QueryDescriptions.PROJECT_NAME));
		properties.put("system", property("string", QueryDescriptions.SOURCE_SYSTEM));
		properties.put("externalId", property("string", QueryDescriptions.SOURCE_EXTERNAL_ID));
		properties.put("otherSystem", property("string", QueryDescriptions.OTHER_SOURCE_SYSTEM));
		properties.put("otherExternalId", property("string",
				QueryDescriptions.OTHER_SOURCE_EXTERNAL_ID));
		return Map.of("type", "object", "properties", properties,
				"required", List.of("projectName", "system", "externalId", "otherSystem",
						"otherExternalId"),
				"additionalProperties", false);
	}

	private Map<String, Object> searchSchema() {
		return Map.of("type", "object",
				"properties", Map.of(
						"projectName", property("string", QueryDescriptions.PROJECT_NAME),
						"query", property("string", QueryDescriptions.SEARCH_QUERY)),
				"required", List.of("projectName", "query"),
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

	private static String optionalText(JsonNode params, String fieldName) {
		JsonNode value = params == null ? null : params.get(fieldName);
		return value == null || value.isNull() || !value.isTextual() ? null : value.asText();
	}

	private static boolean optionalBoolean(JsonNode params, String fieldName, boolean defaultValue) {
		JsonNode value = params == null ? null : params.get(fieldName);
		return value == null || value.isNull() || !value.isBoolean() ? defaultValue
				: value.asBoolean();
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Could not serialize MCP payload", e);
		}
	}
}
