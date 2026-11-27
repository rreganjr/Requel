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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.service.api.dto.TraceGapDto;
import com.rreganjr.requel.service.api.dto.TraceabilityDto;
import com.rreganjr.requel.user.User;

/**
 * #497: the goal matrix and the gaps ({@code GET /api/projects/{name}/traceability} and the
 * gateway), on a project with exactly one of each gap and on a fully linked one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TraceabilityIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private QueryGateway queryGateway;

	private String projectName;
	private String token;
	private String ts;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		ts = Long.toString(System.nanoTime());
		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
	}

	/**
	 * AC1 and AC3: one of each gap kind, each exactly once, beside the near misses that are not
	 * gaps: a sub-scenario under a covered scenario, an actor named only by a step, and a goal
	 * referenced only by a stakeholder (shown in the matrix, not realized).
	 */
	@Test
	void reportsEachGapKindOnceAndTheMatrix() throws Exception {
		newProject("trace-" + ts);
		Fixture f = linkedCore();

		// Gaps, one of each kind.
		long lonely = id(command("EditGoal", Map.of("projectName", projectName, "name", "Lonely goal " + ts,
				"text", "")));
		long sponsor = id(command("EditNonUserStakeholder", Map.of("projectName", projectName,
				"name", "Sponsor " + ts, "text", "")));
		link("AddGoalToGoalContainer", "goalContainerId", sponsor, "goalId", lonely, "Stakeholder");
		long auditor = id(command("EditActor", Map.of("projectName", projectName, "name", "Auditor " + ts)));
		long orphanStory = id(command("EditStory", Map.of("projectName", projectName,
				"name", "Orphan story " + ts, "text", "", "storyTypeName", "Success")));
		long bareUseCase = id(command("EditUseCase", Map.of("projectName", projectName,
				"name", "Bare use case " + ts, "text", "", "primaryActorName", "Clerk " + ts)));
		link("AddGoalToGoalContainer", "goalContainerId", bareUseCase, "goalId", f.goal, "UseCase");
		long goalless = id(command("EditUseCase", Map.of("projectName", projectName,
				"name", "Goalless use case " + ts, "text", "", "primaryActorName", "Clerk " + ts)));
		long strayScenario = id(command("EditScenario", scenario("Stray scenario " + ts,
				List.of(step("wanders off " + ts)))));
		long quiet = id(command("EditNonUserStakeholder", Map.of("projectName", projectName,
				"name", "Quiet " + ts, "text", "")));

		TraceabilityDto trace = asAdmin(() -> queryGateway.getTraceability(projectName));

		Map<String, List<String>> gaps = new HashMap<>();
		trace.gaps().forEach(g -> gaps.computeIfAbsent(g.kind(), k -> new ArrayList<>())
				.add(g.entity().entityType() + ":" + g.entity().id()));
		assertThat(gaps).containsOnlyKeys(TraceGapDto.KINDS);
		assertThat(gaps.get(TraceGapDto.GOAL_NOT_REALIZED)).containsExactly("Goal:" + lonely);
		assertThat(gaps.get(TraceGapDto.ACTOR_NOT_USED)).containsExactly("Actor:" + auditor);
		assertThat(gaps.get(TraceGapDto.STORY_WITHOUT_GOAL)).containsExactly("Story:" + orphanStory);
		assertThat(gaps.get(TraceGapDto.STORY_NOT_IN_USE_CASE)).containsExactly("Story:" + orphanStory);
		assertThat(gaps.get(TraceGapDto.USE_CASE_WITHOUT_GOAL)).containsExactly("UseCase:" + goalless);
		assertThat(gaps.get(TraceGapDto.SCENARIO_NOT_IN_USE_CASE)).containsExactly("Scenario:" + strayScenario);
		// The admin's user stakeholder has no goals either: every stakeholder counts.
		assertThat(gaps.get(TraceGapDto.STAKEHOLDER_WITHOUT_GOAL)).contains("Stakeholder:" + quiet)
				.doesNotContain("Stakeholder:" + sponsor);
		assertThat(trace.gaps()).extracting(TraceGapDto::kind).isSortedAccordingTo(
				(a, b) -> TraceGapDto.KINDS.indexOf(a) - TraceGapDto.KINDS.indexOf(b));

		// The matrix: every goal in creation order, each referer in its column.
		assertThat(trace.goals()).extracting(g -> g.id()).containsExactly(f.goal, lonely);
		var realized = trace.goals().get(0);
		assertThat(realized.realized()).isTrue();
		assertThat(realized.stories()).extracting(r -> r.id()).containsExactly(f.story);
		assertThat(realized.useCases()).extracting(r -> r.id()).containsExactly(bareUseCase, f.useCase);
		assertThat(realized.actors()).extracting(r -> r.id()).containsExactly(f.clerk);
		assertThat(realized.stakeholders()).extracting(r -> r.id()).containsExactly(f.owner);
		var notRealized = trace.goals().get(1);
		assertThat(notRealized.realized()).isFalse();
		assertThat(notRealized.stakeholders()).extracting(r -> r.id()).containsExactly(sponsor);

		// REST answers the same.
		JsonNode rest = ok(mockMvc.perform(get("/api/projects/" + projectName + "/traceability")
				.header("Authorization", "Bearer " + token)));
		assertThat(rest.get("gaps")).hasSize(trace.gaps().size());
		assertThat(rest.get("goals").get(1).get("realized").asBoolean()).isFalse();
	}

	/** AC2: a fully linked project has no gaps (the admin's user stakeholder gets a goal too). */
	@Test
	void aFullyLinkedProjectHasNoGaps() throws Exception {
		newProject("trace-full-" + ts);
		Fixture f = linkedCore();
		JsonNode stakeholders = ok(mockMvc.perform(get("/api/projects/" + projectName + "/stakeholders")
				.header("Authorization", "Bearer " + token)));
		for (JsonNode s : stakeholders) {
			link("AddGoalToGoalContainer", "goalContainerId", s.get("id").asLong(), "goalId", f.goal,
					"Stakeholder");
		}

		TraceabilityDto trace = asAdmin(() -> queryGateway.getTraceability(projectName));

		assertThat(trace.gaps()).isEmpty();
		assertThat(trace.goals()).hasSize(1);
	}

	/** An empty project: no goals, no gaps beyond its stakeholders. */
	@Test
	void anEmptyProjectHasNoGoals() throws Exception {
		newProject("trace-empty-" + ts);
		TraceabilityDto trace = asAdmin(() -> queryGateway.getTraceability(projectName));
		assertThat(trace.goals()).isEmpty();
		assertThat(trace.gaps()).allSatisfy(g ->
				assertThat(g.kind()).isEqualTo(TraceGapDto.STAKEHOLDER_WITHOUT_GOAL));
	}

	// ---- the fully linked core --------------------------------------------------------------------

	/** Ids of a goal, story, use case, actors and stakeholder that all trace. */
	private record Fixture(long goal, long story, long useCase, long clerk, long printer, long owner) {}

	/**
	 * A goal realized by a story and a use case, and held by the actor Clerk, whom the story and use
	 * case list. The use case's scenario has a step Printer does (#468) and a sub-scenario; a named
	 * stakeholder holds the goal.
	 */
	private Fixture linkedCore() throws Exception {
		long goal = id(command("EditGoal", Map.of("projectName", projectName, "name", "Fast checkout " + ts,
				"text", "")));
		long clerk = id(command("EditActor", Map.of("projectName", projectName, "name", "Clerk " + ts)));
		long printer = id(command("EditActor", Map.of("projectName", projectName, "name", "Printer " + ts)));
		long story = id(command("EditStory", Map.of("projectName", projectName, "name", "Clerk scans " + ts,
				"text", "", "storyTypeName", "Success")));
		link("AddGoalToGoalContainer", "goalContainerId", story, "goalId", goal, "Story");
		link("AddActorToActorContainer", "actorContainerId", story, "actorId", clerk, "Story");
		link("AddGoalToGoalContainer", "goalContainerId", clerk, "goalId", goal, "Actor");
		long useCase = id(command("EditUseCase", Map.of("projectName", projectName, "name", "Check out " + ts,
				"text", "", "primaryActorName", "Clerk " + ts)));
		link("AddGoalToGoalContainer", "goalContainerId", useCase, "goalId", goal, "UseCase");
		link("AddActorToActorContainer", "actorContainerId", useCase, "actorId", clerk, "UseCase");
		link("AddStoryToStoryContainer", "storyContainerId", useCase, "storyId", story, "UseCase");
		long sub = id(command("EditScenario", scenario("Print receipt " + ts, List.of(
				printerStep("prints the receipt " + ts, "Printer " + ts)))));
		long main = id(command("EditScenario", scenario("Pay " + ts, List.of(
				step("takes payment " + ts), Map.of("stepId", sub, "isScenario", true)))));
		ok(command("AddScenarioToUseCase", Map.of("projectName", projectName, "useCaseId", useCase,
				"scenarioId", main)));
		long owner = id(command("EditNonUserStakeholder", Map.of("projectName", projectName,
				"name", "Owner " + ts, "text", "")));
		link("AddGoalToGoalContainer", "goalContainerId", owner, "goalId", goal, "Stakeholder");
		// A use case with its own scenario must not leave that scenario a gap.
		for (JsonNode s : ok(mockMvc.perform(get("/api/projects/" + projectName + "/scenarios")
				.header("Authorization", "Bearer " + token)))) {
			long scenarioId = s.get("id").asLong();
			if (scenarioId != sub && scenarioId != main) {
				ok(command("AddScenarioToUseCase", Map.of("projectName", projectName, "useCaseId", useCase,
						"scenarioId", scenarioId)));
			}
		}
		return new Fixture(goal, story, useCase, clerk, printer, owner);
	}

	// ---- helpers --------------------------------------------------------------------------------

	private void newProject(String name) throws Exception {
		projectName = name;
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(admin);
		projectCommand.setName(name);
		projectCommand.setText("traceability test");
		projectCommand.setOrganizationName("TraceOrg-" + name);
		getCommandHandler().execute(projectCommand);
	}

	private void link(String type, String containerKey, long containerId, String itemKey, long itemId,
			String containerType) throws Exception {
		ok(command(type, Map.of("projectName", projectName, containerKey, containerId, itemKey, itemId,
				"containerType", containerType)));
	}

	private Map<String, Object> scenario(String name, List<Map<String, Object>> steps) {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("name", name);
		input.put("text", "");
		input.put("scenarioTypeName", "Primary");
		input.put("steps", steps);
		return input;
	}

	private static Map<String, Object> step(String name) {
		return Map.of("name", name, "isScenario", false);
	}

	private static Map<String, Object> printerStep(String name, String actorName) {
		return Map.of("name", name, "isScenario", false, "actorRole", "NAMED", "actorName", actorName);
	}

	private static <T> T asAdmin(Supplier<T> call) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", "x", List.of()));
		try {
			return call.get();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private ResultActions command(String type, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private long id(ResultActions result) throws Exception {
		return ok(result).get("id").asLong();
	}

	private JsonNode ok(ResultActions result) throws Exception {
		String body = result.andReturn().getResponse().getContentAsString();
		int statusCode = result.andReturn().getResponse().getStatus();
		assertThat(statusCode).as(body).isEqualTo(200);
		JsonNode json = objectMapper.readTree(body);
		return json.has("entity") && json.has("success") ? json.get("entity") : json;
	}
}
