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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpWriteServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final GatewayCommandCatalog catalog = McpTestCatalog.sample();

	/** Records the request it receives and returns a canned result (or throws a set exception). */
	private static final class RecordingGateway implements CommandGateway {
		GatewayRequest last;
		GatewayException toThrow;
		Object resultPayload = Map.of("id", 1L, "name", "G");

		@Override
		public GatewayResult execute(GatewayRequest request) throws GatewayException {
			this.last = request;
			if (toThrow != null) {
				throw toThrow;
			}
			return new GatewayResult(request.commandType(), resultPayload);
		}
	}

	private JsonNode json(String raw) {
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object value) {
		return (Map<String, Object>) value;
	}

	// ---- opt-in flag ---------------------------------------------------------------------------

	@Test
	void writeToolsAbsentAndRejectedWhenDisabled() {
		McpWriteService disabled = new McpWriteService(new RecordingGateway(), catalog,
				objectMapper, false);
		assertThat(disabled.toolDescriptors()).isEmpty();
		assertThat(disabled.handles("EditGoal")).isTrue();
		assertThatThrownBy(() -> disabled.call("EditGoal",
				json("{\"projectName\":\"P\",\"name\":\"G\"}")))
				.isInstanceOf(McpInvalidParamsException.class)
				.hasMessageContaining("disabled");
	}

	@Test
	void readServiceOmitsWriteToolsWhenDisabled() {
		McpReadService readOnly = new McpReadService(new StubProjectQueryGateway(), objectMapper);
		Map<String, Object> tools = readOnly.listTools();
		@SuppressWarnings("unchecked")
		List<McpToolDescriptor> descriptors = (List<McpToolDescriptor>) tools.get("tools");
		assertThat(descriptors).noneMatch(d -> d.name().startsWith("runCommand")
				|| d.name().equals("EditGoal"));
	}

	@Test
	void writeToolsListedWhenEnabled() {
		McpWriteService enabled = new McpWriteService(new RecordingGateway(), catalog,
				objectMapper, true);
		assertThat(enabled.toolDescriptors()).extracting(McpToolDescriptor::name)
				.contains("runCommand", "EditProject", "EditGoal",
						"AddGoalToGoalContainer", "EditNote", "EditIssue");
	}

	// ---- delegation ----------------------------------------------------------------------------

	@Test
	void runCommandForwardsTypeAndInput() {
		RecordingGateway gw = new RecordingGateway();
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		Object result = svc.call("runCommand",
				json("{\"commandType\":\"EditGoal\",\"input\":{\"projectName\":\"P\",\"name\":\"G\"}}"));
		assertThat(gw.last.commandType()).isEqualTo("EditGoal");
		assertThat(gw.last.input()).isInstanceOf(Map.class);
		assertThat(asMap(gw.last.input())).containsEntry("projectName", "P")
				.containsEntry("name", "G");
		assertThat(result).isEqualTo(gw.resultPayload);
	}

	@Test
	void clientIdFromContextIsCarriedIntoGatewayRequest() {
		RecordingGateway gw = new RecordingGateway();
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		McpClientContext.setClientId("claude-desktop");
		try {
			svc.call("EditGoal", json("{\"projectName\":\"P\",\"name\":\"G\"}"));
		} finally {
			McpClientContext.clear();
		}
		assertThat(gw.last.clientId()).isEqualTo("claude-desktop");
	}

	@Test
	void typedToolFixesCommandTypeAndForwardsArgs() {
		RecordingGateway gw = new RecordingGateway();
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		svc.call("EditGoal",
				json("{\"projectName\":\"P\",\"name\":\"G\",\"text\":\"T\"}"));
		assertThat(gw.last.commandType()).isEqualTo("EditGoal");
		assertThat(asMap(gw.last.input()))
				.containsEntry("projectName", "P")
				.containsEntry("name", "G")
				.containsEntry("text", "T");
	}

	@Test
	void voidResultBecomesOkPayload() {
		RecordingGateway gw = new RecordingGateway();
		gw.resultPayload = null; // e.g. AddGoalToGoalContainer returns no DTO
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		Object result = svc.call("AddGoalToGoalContainer",
				json("{\"projectName\":\"P\",\"goalId\":1,\"goalContainerId\":2,"
						+ "\"containerType\":\"Project\"}"));
		assertThat(asMap(result)).containsEntry("ok", true)
				.containsEntry("commandType", "AddGoalToGoalContainer");
	}

	// ---- error mapping -------------------------------------------------------------------------

	@Test
	void notAllowedMapsToInvalidParams() {
		RecordingGateway gw = new RecordingGateway();
		gw.toThrow = new GatewayException(GatewayException.Kind.NOT_ALLOWED, "denied");
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		assertThatThrownBy(() -> svc.call("EditGoal",
				json("{\"projectName\":\"P\",\"name\":\"G\"}")))
				.isInstanceOf(McpInvalidParamsException.class)
				.hasMessageContaining("denied");
	}

	@Test
	void executionErrorMapsToIllegalState() {
		RecordingGateway gw = new RecordingGateway();
		gw.toThrow = new GatewayException(GatewayException.Kind.EXECUTION_ERROR, "boom");
		McpWriteService svc = new McpWriteService(gw, catalog, objectMapper, true);
		assertThatThrownBy(() -> svc.call("EditGoal",
				json("{\"projectName\":\"P\",\"name\":\"G\"}")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("boom");
	}
}
