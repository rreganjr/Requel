/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;

/**
 * #468: a step's (and a scenario's) actor as a role, through the API: set and cleared with
 * EditScenario and EditScenarioStep, read back with its displayName (rendered per use case), kept
 * by copy and convert, and released to plain wording when its named actor is deleted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StepActorRoleApiIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String projectName;
	private String token;
	private String ts;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		ts = Long.toString(System.nanoTime());
		projectName = "step-actor-role-" + ts;
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(admin);
		projectCommand.setName(projectName);
		projectCommand.setText("step actor role test");
		projectCommand.setOrganizationName("StepActorRoleOrg-" + ts);
		getCommandHandler().execute(projectCommand);

		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
		for (String actor : List.of("Operator", "Conduit Engineer", "Clerk")) {
			ok(command("EditActor", Map.of("projectName", projectName, "name", actor)));
		}
	}

	/** AC1 through the API: one step, read with each use case's own primary actor. */
	@Test
	void aPrimaryActorStepReadsPerUseCaseAndStaysOneStep() throws Exception {
		JsonNode scenario = ok(command("EditScenario", scenarioInput("ends a room " + ts,
				"PRIMARY_ACTOR", null, List.of(step("ends the room " + ts, "PRIMARY_ACTOR", null),
						step("logs the end " + ts, "SYSTEM", null),
						step("approves it " + ts, null, "Clerk")))));
		long scenarioId = scenario.get("id").asLong();
		assertThat(scenario.get("actorRole").asText()).isEqualTo("PRIMARY_ACTOR");
		assertThat(scenario.get("name").asText()).isEqualTo("ends a room " + ts);
		JsonNode steps = scenario.get("steps");
		assertThat(steps.get(0).get("displayName").asText())
				.isEqualTo("Primary actor ends the room " + ts);
		assertThat(steps.get(1).get("displayName").asText()).isEqualTo("The system logs the end " + ts);
		assertThat(steps.get(2).get("actorRole").asText()).isEqualTo("NAMED");
		assertThat(steps.get(2).get("actorName").asText()).isEqualTo("Clerk");
		assertThat(steps.get(2).get("displayName").asText()).isEqualTo("Clerk approves it " + ts);

		long operatorCase = useCase("Run a session " + ts, "Operator", scenarioId);
		long engineerCase = useCase("Respond to an alarm " + ts, "Conduit Engineer", scenarioId);

		assertThat(additionalScenario(operatorCase).get("displayName").asText())
				.isEqualTo("Operator ends a room " + ts);
		assertThat(additionalScenario(engineerCase).get("displayName").asText())
				.isEqualTo("Conduit Engineer ends a room " + ts);
		JsonNode read = ok(read("/scenarios/" + scenarioId));
		assertThat(read.get("steps").get(0).get("displayName").asText())
				.as("two use cases with different primary actors read as the role")
				.isEqualTo("Primary actor ends the room " + ts);
		long stepId = read.get("steps").get(0).get("id").asLong();
		assertThat(ok(read("/steps/" + stepId)).get("usedIn")).hasSize(1);
	}

	@Test
	void editScenarioStepSetsAndClearsTheRole() throws Exception {
		long stepId = firstStepId(ok(command("EditScenario", scenarioInput("Close out " + ts,
				null, null, List.of(step("Close the books " + ts, null, null))))));

		JsonNode set = ok(command("EditScenarioStep", Map.of("projectName", projectName,
				"stepId", stepId, "name", "closes the books " + ts, "actorRole", "SYSTEM")));
		assertThat(set.get("actorRole").asText()).isEqualTo("SYSTEM");
		assertThat(set.get("displayName").asText()).isEqualTo("The system closes the books " + ts);

		JsonNode cleared = ok(command("EditScenarioStep", Map.of("projectName", projectName,
				"stepId", stepId, "name", "Clerk closes the books " + ts, "actorRole", "NONE")));
		assertThat(cleared.has("actorRole")).isFalse();
		assertThat(cleared.get("displayName").asText()).isEqualTo("Clerk closes the books " + ts);

		command("EditScenarioStep", Map.of("projectName", projectName, "stepId", stepId,
				"actorName", "Nobody")).andExpect(status().is4xxClientError());
		command("EditScenarioStep", Map.of("projectName", projectName, "stepId", stepId,
				"actorRole", "SOMEONE")).andExpect(status().is4xxClientError());
	}

	@Test
	void copyAndConvertKeepTheRole() throws Exception {
		JsonNode scenario = ok(command("EditScenario", scenarioInput("Hand over " + ts, null,
				null, List.of(step("hands over the key " + ts, "PRIMARY_ACTOR", null)))));
		JsonNode copy = ok(command("CopyScenario", Map.of("projectName", projectName,
				"scenarioId", scenario.get("id").asLong())));
		assertThat(copy.get("steps").get(0).get("actorRole").asText()).isEqualTo("PRIMARY_ACTOR");

		JsonNode converted = ok(command("ConvertStepToScenario", Map.of("projectName",
				projectName, "stepId", firstStepId(scenario))));
		assertThat(converted.get("actorRole").asText()).isEqualTo("PRIMARY_ACTOR");
		assertThat(converted.get("name").asText()).isEqualTo("hands over the key " + ts);
	}

	/** Decision 5: deleting a named actor turns its steps back into plain wording. */
	@Test
	void deletingANamedActorReturnsItsStepsToPlainWording() throws Exception {
		long actorId = ok(command("EditActor", Map.of("projectName", projectName, "name",
				"Auditor " + ts))).get("id").asLong();
		long stepId = firstStepId(ok(command("EditScenario", scenarioInput("Audit " + ts, null,
				null, List.of(step("signs off " + ts, null, "Auditor " + ts))))));

		ok(command("DeleteActor", Map.of("projectName", projectName, "actorId", actorId)));

		JsonNode step = ok(read("/steps/" + stepId));
		assertThat(step.get("name").asText()).isEqualTo("Auditor " + ts + " signs off " + ts);
		assertThat(step.has("actorRole")).isFalse();
	}

	@Test
	void deletingANamedActorIsRefusedWhenThePlainWordingIsTaken() throws Exception {
		long actorId = ok(command("EditActor", Map.of("projectName", projectName, "name",
				"Notary " + ts))).get("id").asLong();
		ok(command("EditScenario", scenarioInput("Notarize " + ts, null, null,
				List.of(step("stamps it " + ts, null, "Notary " + ts),
						step("Notary " + ts + " stamps it " + ts, null, null)))));

		command("DeleteActor", Map.of("projectName", projectName, "actorId", actorId))
				.andExpect(status().is4xxClientError());
	}

	// ---- helpers --------------------------------------------------------------------------------

	private long useCase(String name, String primaryActor, long scenarioId) throws Exception {
		long id = ok(command("EditUseCase", Map.of("projectName", projectName, "name", name,
				"text", "", "primaryActorName", primaryActor))).get("id").asLong();
		ok(command("AddScenarioToUseCase", Map.of("projectName", projectName, "useCaseId", id,
				"scenarioId", scenarioId)));
		return id;
	}

	private JsonNode additionalScenario(long useCaseId) throws Exception {
		return ok(read("/use-cases/" + useCaseId)).get("additionalScenarios").get(0);
	}

	private static long firstStepId(JsonNode scenario) {
		return scenario.get("steps").get(0).get("id").asLong();
	}

	private Map<String, Object> scenarioInput(String name, String role, String actorName,
			List<Map<String, Object>> steps) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("projectName", projectName);
		input.put("name", name);
		input.put("text", "");
		input.put("scenarioTypeName", "Primary");
		if (role != null) {
			input.put("actorRole", role);
		}
		if (actorName != null) {
			input.put("actorName", actorName);
		}
		input.put("steps", new ArrayList<>(steps));
		return input;
	}

	private static Map<String, Object> step(String name, String role, String actorName) {
		Map<String, Object> step = new LinkedHashMap<>();
		step.put("name", name);
		step.put("isScenario", false);
		if (role != null) {
			step.put("actorRole", role);
		}
		if (actorName != null) {
			step.put("actorName", actorName);
		}
		return step;
	}

	private ResultActions command(String type, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private ResultActions read(String path) throws Exception {
		return mockMvc.perform(get("/api/projects/" + projectName + path)
				.header("Authorization", "Bearer " + token));
	}

	private JsonNode ok(ResultActions result) throws Exception {
		String body = result.andReturn().getResponse().getContentAsString();
		int statusCode = result.andReturn().getResponse().getStatus();
		assertThat(statusCode).as(body).isEqualTo(200);
		JsonNode json = objectMapper.readTree(body);
		// a command answers {success, entity, ...}; a read answers the entity itself
		return json.has("entity") && json.has("success") ? json.get("entity") : json;
	}
}
