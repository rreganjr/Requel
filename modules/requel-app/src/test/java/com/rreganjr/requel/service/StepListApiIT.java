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

import java.util.HashMap;
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
 * #505: the steps list ({@code GET /api/projects/{name}/steps}) with each step's usage and open
 * issues, and the project's plain-step count on the project read and the project list.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StepListApiIT extends AbstractIntegrationTestCase {

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
		projectName = "step-list-" + ts;
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(admin);
		projectCommand.setName(projectName);
		projectCommand.setText("step list test");
		projectCommand.setOrganizationName("StepListOrg-" + ts);
		getCommandHandler().execute(projectCommand);

		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
	}

	/**
	 * AC1 and AC5: a shared step, a single-use step and a step with a HIGH open issue are listed
	 * with their usage and issues; the sub-scenario is in neither the list nor the count.
	 */
	@Test
	void listsPlainStepsWithUsageAndOpenIssuesAndCountsThem() throws Exception {
		JsonNode inner = ok(command("EditScenario", scenario("Check stock " + ts,
				List.of(newStep("counts the shelf " + ts)))));
		JsonNode first = ok(command("EditScenario", scenario("Take an order " + ts, List.of(
				newStep("enters the order " + ts),
				newStep("prints a receipt " + ts),
				Map.of("stepId", inner.get("id").asLong(), "isScenario", true)))));
		long sharedId = stepNamed(first, "enters the order " + ts);
		long flaggedId = stepNamed(first, "prints a receipt " + ts);
		ok(command("EditScenario", scenario("Change an order " + ts,
				List.of(Map.of("stepId", sharedId)))));
		ok(command("EditIssue", Map.of("projectName", projectName, "entityType", "Step",
				"entityId", flaggedId, "text", "Which printer?", "mustBeResolved", false,
				"severity", "LOW")));
		ok(command("EditIssue", Map.of("projectName", projectName, "entityType", "Step",
				"entityId", flaggedId, "text", "Paper or email?", "mustBeResolved", true,
				"severity", "HIGH")));

		Map<String, JsonNode> byName = new HashMap<>();
		ok(read("/steps")).forEach(s -> byName.put(s.get("name").asText(), s));

		assertThat(byName.keySet()).containsExactlyInAnyOrder("enters the order " + ts,
				"prints a receipt " + ts, "counts the shelf " + ts);
		JsonNode shared = byName.get("enters the order " + ts);
		assertThat(shared.get("usedIn").asInt()).isEqualTo(2);
		assertThat(shared.get("openIssues").asInt()).isZero();
		assertThat(shared.has("highestOpenSeverity")).isFalse();
		JsonNode flagged = byName.get("prints a receipt " + ts);
		assertThat(flagged.get("usedIn").asInt()).isEqualTo(1);
		assertThat(flagged.get("openIssues").asInt()).isEqualTo(2);
		assertThat(flagged.get("highestOpenSeverity").asText()).isEqualTo("HIGH");
		assertThat(byName.get("counts the shelf " + ts).get("usedIn").asInt()).isEqualTo(1);

		assertThat(ok(read("")).get("stepCount").asInt()).isEqualTo(3);
		JsonNode listed = null;
		for (JsonNode p : ok(mockMvc.perform(get("/api/projects")
				.header("Authorization", "Bearer " + token)))) {
			if (projectName.equals(p.get("name").asText())) {
				listed = p;
			}
		}
		assertThat(listed).isNotNull();
		assertThat(listed.get("stepCount").asInt()).isEqualTo(3);
	}

	/** A resolved issue no longer counts. */
	@Test
	void aResolvedIssueIsNotCounted() throws Exception {
		JsonNode scenario = ok(command("EditScenario", scenario("Close out " + ts,
				List.of(newStep("closes the till " + ts)))));
		long stepId = stepNamed(scenario, "closes the till " + ts);
		long issueId = ok(command("EditIssue", Map.of("projectName", projectName, "entityType",
				"Step", "entityId", stepId, "text", "Who counts?", "mustBeResolved", false,
				"severity", "MEDIUM"))).get("id").asLong();
		long positionId = ok(command("EditPosition", Map.of("projectName", projectName,
				"issueId", issueId, "text", "The manager"))).get("id").asLong();
		ok(command("ResolveIssue", Map.of("projectName", projectName, "issueId", issueId,
				"positionId", positionId)));

		JsonNode step = ok(read("/steps")).get(0);

		assertThat(step.get("openIssues").asInt()).isZero();
		assertThat(step.has("highestOpenSeverity")).isFalse();
	}

	/** An empty project has no steps and a count of 0. */
	@Test
	void anEmptyProjectHasNoSteps() throws Exception {
		assertThat(ok(read("/steps"))).isEmpty();
		assertThat(ok(read("")).get("stepCount").asInt()).isZero();
	}

	// ---- helpers --------------------------------------------------------------------------------

	private Map<String, Object> scenario(String name, List<Map<String, Object>> steps) {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("name", name);
		input.put("text", "");
		input.put("scenarioTypeName", "Primary");
		input.put("steps", steps);
		return input;
	}

	private static Map<String, Object> newStep(String name) {
		return Map.of("name", name, "isScenario", false);
	}

	private static long stepNamed(JsonNode scenario, String name) {
		for (JsonNode step : scenario.get("steps")) {
			if (name.equals(step.get("name").asText())) {
				return step.get("id").asLong();
			}
		}
		throw new AssertionError("no step named " + name);
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
