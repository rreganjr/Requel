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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.QueryDescriptions;

class McpReadServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final McpReadService service = new McpReadService(new StubProjectQueryGateway(),
			objectMapper);

	@Test
	void rateLimiterIsConsultedForEveryToolCall() {
		McpRateLimiter blocking = (clientId, toolName) -> {
			throw new McpRateLimitExceededException("over limit for " + toolName);
		};
		McpReadService limited = new McpReadService(new StubProjectQueryGateway(),
				new McpWriteService(null, GatewayCommandCatalog.empty(), objectMapper, false),
				blocking, objectMapper);
		assertThatThrownBy(() -> limited.callTool(json("""
				{ "name": "getProject", "arguments": { "projectName": "Sample" } }
				""")))
				.isInstanceOf(McpRateLimitExceededException.class)
				.hasMessageContaining("getProject");
	}

	@Test
	void callsReadOnlyProjectTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "getProject",
				  "arguments": { "projectName": "Sample" }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("type").asText()).isEqualTo("text");
		assertThat(content.get(0).path("text").asText()).contains("\"name\" : \"Sample\"");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsOpenIssuesTool() {
		Map<String, Object> response = service.callTool(json("""
				{ "name": "getOpenIssues", "arguments": { "projectName": "Sample" } }
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("What is the SLA?");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsGetAnnotationsTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "getAnnotations",
				  "arguments": { "projectName": "Sample", "entityType": "Goal", "entityId": 10 }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("type").asText()).isEqualTo("text");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsGetEntityTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "getEntity",
				  "arguments": { "projectName": "Sample", "entityType": "Goal", "entityId": 10 }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("\"name\" : \"Improve login\"");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsGetEntityNeighborsTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "getEntityNeighbors",
				  "arguments": { "projectName": "Sample", "entityType": "Goal", "entityId": 10 }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("Login story");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsSearchProjectEntitiesTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "searchProjectEntities",
				  "arguments": { "projectName": "Sample", "query": "login" }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("Improve login");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsGetProjectContextTool() {
		Map<String, Object> response = service.callTool(json("""
				{ "name": "getProjectContext", "arguments": { "projectName": "Sample" } }
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		String text = content.get(0).path("text").asText();
		assertThat(text).contains("\"project\"").contains("\"Sample\"").contains("\"openIssues\"");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void draftAnnotationReturnsUnpersistedDraft() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "draftAnnotation",
				  "arguments": {
				    "entityType": "Goal", "entityId": 10, "kind": "ISSUE",
				    "text": "Clarify the SLA"
				  }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		String text = content.get(0).path("text").asText();
		assertThat(text).contains("CREATE_OR_UPDATE_ISSUE").contains("Clarify the SLA")
				.contains("Goal");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void draftAnnotationRejectsUnknownKind() {
		assertThatThrownBy(() -> service.callTool(json("""
				{
				  "name": "draftAnnotation",
				  "arguments": { "entityType": "Goal", "entityId": 10, "kind": "BOGUS",
				    "text": "x" }
				}
				""")))
				.isInstanceOf(McpInvalidParamsException.class);
	}

	@Test
	void draftAnnotationNormalisesSeverity() {
		// #271: case-insensitive, and the draft carries the upper-case name EditIssue stores.
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "draftAnnotation",
				  "arguments": {
				    "entityType": "Goal", "entityId": 10, "kind": "ISSUE",
				    "text": "Admin console may be open to the whole tenant", "severity": "high"
				  }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		JsonNode draft = json(content.get(0).path("text").asText());
		assertThat(draft.path("severity").asText()).isEqualTo("HIGH");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void draftAnnotationWithoutSeverityLeavesItNull() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "draftAnnotation",
				  "arguments": { "entityType": "Goal", "entityId": 10, "kind": "ISSUE",
				    "text": "Clarify the SLA" }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		JsonNode draft = json(content.get(0).path("text").asText());
		assertThat(draft.path("severity").isNull() || draft.path("severity").isMissingNode())
				.isTrue();
	}

	/**
	 * Issue #296: an issue draft's mustResolve defaults to false, as EditIssue's mustBeResolved
	 * does, so a draft saved as-is makes the issue it describes. It used to default to true.
	 */
	@Test
	void draftAnnotationDefaultsMustResolveToFalseAndKeepsAnExplicitValue() {
		JsonNode defaulted = draft("""
				{ "entityType": "Goal", "entityId": 10, "kind": "ISSUE", "text": "Clarify the SLA" }
				""");
		assertThat(defaulted.at("/metadata/mustResolve").asBoolean(true)).isFalse();

		JsonNode explicit = draft("""
				{ "entityType": "Goal", "entityId": 10, "kind": "ISSUE", "text": "Clarify the SLA",
				  "mustResolve": true }
				""");
		assertThat(explicit.at("/metadata/mustResolve").asBoolean(false)).isTrue();
	}

	private JsonNode draft(String arguments) {
		Map<String, Object> response = service.callTool(json(
				"{ \"name\": \"draftAnnotation\", \"arguments\": " + arguments + " }"));
		JsonNode content = objectMapper.valueToTree(response.get("content"));
		return json(content.get(0).path("text").asText());
	}

	@Test
	void draftAnnotationRejectsUnknownSeverity() {
		// #271: an unrecognised severity is an error, not a silent drop.
		assertThatThrownBy(() -> service.callTool(json("""
				{
				  "name": "draftAnnotation",
				  "arguments": { "entityType": "Goal", "entityId": 10, "kind": "ISSUE",
				    "text": "x", "severity": "urgent" }
				}
				""")))
				.isInstanceOf(McpInvalidParamsException.class)
				.hasMessageContaining("urgent");
	}

	@Test
	@SuppressWarnings("unchecked")
	void draftAnnotationSchemaListsTheSeverityVocabulary() {
		List<McpToolDescriptor> tools = (List<McpToolDescriptor>) service.listTools().get("tools");
		McpToolDescriptor draft = tools.stream()
				.filter(t -> "draftAnnotation".equals(t.name())).findFirst().orElseThrow();
		JsonNode schema = objectMapper.valueToTree(draft.inputSchema());
		assertThat(schema.at("/properties/severity/enum").toString())
				.isEqualTo("[\"LOW\",\"MEDIUM\",\"HIGH\"]");
	}

	@SuppressWarnings("unchecked")
	private List<McpToolDescriptor> readTools() {
		return (List<McpToolDescriptor>) service.listTools().get("tools");
	}

	private JsonNode schemaOf(String toolName) {
		McpToolDescriptor tool = readTools().stream().filter(t -> toolName.equals(t.name()))
				.findFirst().orElseThrow();
		return objectMapper.valueToTree(tool.inputSchema());
	}

	/**
	 * Issue #296: every read tool reaches a caller with a real description. This service is
	 * built read-only, so {@code listTools()} returns exactly the read tools; a new one (#274,
	 * #72) added without text fails here.
	 */
	@Test
	void everyReadToolIsDescribed() {
		assertThat(readTools()).isNotEmpty().allSatisfy(tool -> assertThat(tool.description())
				.as(tool.name()).isNotBlank().hasSizeGreaterThan(40).endsWith("."));
	}

	/** Issue #296: every property of every read tool's input schema says what it is. */
	@Test
	void everyReadToolPropertyIsDescribed() {
		for (McpToolDescriptor tool : readTools()) {
			JsonNode properties = objectMapper.valueToTree(tool.inputSchema()).path("properties");
			properties.fields().forEachRemaining(property -> assertThat(
					property.getValue().path("description").asText())
					.as(tool.name() + "." + property.getKey()).isNotBlank());
		}
	}

	/**
	 * Issue #296: {@code entityType} is an enum where the accepted set is fixed. The detail reads
	 * accept only the types {@code InProcessQueryGateway.getEntity} switches on; the annotation
	 * reads accept every annotatable type. {@code McpToolCatalogLockstepIT} pins both lists to
	 * the running application.
	 */
	@Test
	void entityTypeIsAnEnumOfTheTypesEachToolAccepts() {
		for (String tool : List.of("getEntity", "getEntityNeighbors")) {
			assertThat(schemaOf(tool).at("/properties/entityType/enum").toString()).as(tool)
					.isEqualTo(objectMapper.valueToTree(QueryDescriptions.READABLE_ENTITY_TYPES)
							.toString());
		}
		for (String tool : List.of("getAnnotations", "draftAnnotation")) {
			assertThat(schemaOf(tool).at("/properties/entityType/enum").toString()).as(tool)
					.isEqualTo(objectMapper.valueToTree(QueryDescriptions.ANNOTATABLE_ENTITY_TYPES)
							.toString());
		}
	}

	/**
	 * The entityType descriptions spell the lists out in prose because the CLI shows them as
	 * picocli annotation text, which must be a constant; this keeps the prose and the lists in step.
	 */
	@Test
	void entityTypeDescriptionsNameEveryAcceptedType() {
		assertThat(QueryDescriptions.READABLE_ENTITY_TYPE)
				.contains(QueryDescriptions.READABLE_ENTITY_TYPES);
		assertThat(QueryDescriptions.ANNOTATABLE_ENTITY_TYPE)
				.contains(QueryDescriptions.ANNOTATABLE_ENTITY_TYPES);
	}

	/** Picocli formats description text, so a stray per-cent sign would break the CLI help. */
	@Test
	void readDescriptionsHoldNoPercentSign() throws IllegalAccessException {
		for (java.lang.reflect.Field field : QueryDescriptions.class.getFields()) {
			if (field.getType() == String.class) {
				assertThat((String) field.get(null)).as(field.getName()).doesNotContain("%");
			}
		}
	}

	private JsonNode json(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
