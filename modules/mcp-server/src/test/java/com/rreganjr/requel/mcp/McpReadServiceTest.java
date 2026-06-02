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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class McpReadServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final McpReadService service = new McpReadService(new StubProjectQueryGateway(),
			objectMapper);

	@Test
	void callsReadOnlyProjectTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "requel.getProject",
				  "arguments": { "projectName": "Sample" }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("type").asText()).isEqualTo("text");
		assertThat(content.get(0).path("text").asText()).contains("\"name\" : \"Sample\"");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void readsProjectTreeResource() {
		Map<String, Object> response = service.readResource(json("""
				{ "uri": "requel://projects/Sample/tree" }
				"""));

		JsonNode contents = objectMapper.valueToTree(response.get("contents"));
		assertThat(contents.get(0).path("mimeType").asText()).isEqualTo("application/json");
		assertThat(contents.get(0).path("text").asText()).contains("\"Goals\"");
	}

	@Test
	void listsProjectsResource() {
		Map<String, Object> response = service.readResource(json("""
				{ "uri": "requel://projects" }
				"""));

		JsonNode contents = objectMapper.valueToTree(response.get("contents"));
		assertThat(contents.get(0).path("text").asText()).contains("\"Sample\"");
	}

	@Test
	void callsOpenIssuesTool() {
		Map<String, Object> response = service.callTool(json("""
				{ "name": "requel.getOpenIssues", "arguments": { "projectName": "Sample" } }
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("What is the SLA?");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void callsGetAnnotationsTool() {
		Map<String, Object> response = service.callTool(json("""
				{
				  "name": "requel.getAnnotations",
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
				  "name": "requel.getEntity",
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
				  "name": "requel.getEntityNeighbors",
				  "arguments": { "projectName": "Sample", "entityType": "Goal", "entityId": 10 }
				}
				"""));

		JsonNode content = objectMapper.valueToTree(response.get("content"));
		assertThat(content.get(0).path("text").asText()).contains("Login story");
		assertThat(response.get("isError")).isEqualTo(false);
	}

	@Test
	void readsOpenIssuesResource() {
		Map<String, Object> response = service.readResource(json("""
				{ "uri": "requel://projects/Sample/open-issues" }
				"""));

		JsonNode contents = objectMapper.valueToTree(response.get("contents"));
		assertThat(contents.get(0).path("text").asText()).contains("What is the SLA?");
	}

	private JsonNode json(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
