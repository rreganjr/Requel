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
import com.rreganjr.requel.service.api.dto.ProjectSearchResultDto;
import com.rreganjr.requel.user.User;

/**
 * #504: searching a project through {@code GET /api/projects/{name}/search} and the gateway. Each
 * test searches for a token unique to its run (the H2 database is shared by every IT), so only
 * rows this test created can match.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProjectSearchIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private QueryGateway queryGateway;

	private String projectName;
	private String token;
	private String ts;
	/** A word no other test writes: what each test searches for. */
	private String word;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		ts = Long.toString(System.nanoTime());
		word = "zq" + ts;
		projectName = newProject("project-search-" + ts);
		token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();
	}

	/** AC1 and AC2: a goal's name, a story's text and an issue's text, the goal first. */
	@Test
	void findsAGoalNameAStoryTextAndAnIssueTextGoalFirst() throws Exception {
		long goalId = id(ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Speed up " + word, "text", "nothing to see"))));
		long storyId = id(ok(command("EditStory", Map.of("projectName", projectName,
				"name", "A clerk files a form " + ts, "text", "Then the " + word + " is saved.",
				"storyTypeName", "Success"))));
		long issueId = issueOnGoal(goalId, "Is the " + word + " measured anywhere?");

		JsonNode page = search(word, null, null, null);

		assertThat(page.get("total").asInt()).isEqualTo(3);
		JsonNode results = page.get("results");
		assertThat(results.get(0).get("entityType").asText()).isEqualTo("Goal");
		assertThat(results.get(0).get("id").asLong()).isEqualTo(goalId);
		assertThat(results.get(0).get("matchedField").asText()).isEqualTo("name");
		assertThat(types(results)).containsExactly("Goal", "Story", "Issue");

		JsonNode story = results.get(1);
		assertThat(story.get("id").asLong()).isEqualTo(storyId);
		assertThat(story.get("matchedField").asText()).isEqualTo("text");
		String snippet = story.get("snippet").asText();
		int start = story.get("matchStart").asInt();
		assertThat(snippet.substring(start, start + story.get("matchLength").asInt()))
				.isEqualTo(word);

		JsonNode issue = results.get(2);
		assertThat(issue.get("id").asLong()).isEqualTo(issueId);
		assertThat(issue.get("resolved").asBoolean()).isFalse();
		assertThat(issue.get("onEntityType").asText()).isEqualTo("Goal");
		assertThat(issue.get("onEntityId").asLong()).isEqualTo(goalId);
		assertThat(issue.get("onEntityName").asText()).isEqualTo("Speed up " + word);
	}

	/** Decision: positions are not searched, and a resolved issue still is, marked resolved. */
	@Test
	void positionTextIsNotSearchedAndAResolvedIssueIsMarked() throws Exception {
		long goalId = id(ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Plain goal " + ts, "text", ""))));
		long issueId = issueOnGoal(goalId, "Which " + word + " wins?");
		long positionId = id(ok(command("EditPosition", Map.of("projectName", projectName,
				"issueId", issueId, "text", "Pick the newest " + word + " " + ts))));
		ok(command("ResolveIssue", Map.of("projectName", projectName, "issueId", issueId,
				"positionId", positionId)));

		JsonNode results = search(word, null, null, null).get("results");

		assertThat(types(results)).containsExactly("Issue");
		assertThat(results.get(0).get("resolved").asBoolean()).isTrue();
	}

	/** AC3: case is ignored and % and _ are matched literally. */
	@Test
	void ignoresCaseAndTakesWildcardsLiterally() throws Exception {
		ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Cut " + word.toUpperCase() + " by 50%", "text", "")));
		ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Cut " + word + " by 50 points", "text", "")));
		ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Rename " + word + "_x", "text", "")));
		ok(command("EditGoal", Map.of("projectName", projectName,
				"name", "Rename " + word + "yx", "text", "")));

		assertThat(search(word, null, null, null).get("total").asInt()).isEqualTo(4);
		assertThat(names(search(word.toUpperCase() + " by 50%", null, null, null).get("results")))
				.containsExactly("Cut " + word.toUpperCase() + " by 50%");
		assertThat(names(search(word + "_x", null, null, null).get("results")))
				.containsExactly("Rename " + word + "_x");
	}

	/** AC4: fewer than 2 characters is refused with a message, on the API and the gateway. */
	@Test
	void aOneCharacterSearchIsRefused() throws Exception {
		ResultActions refused = mockMvc.perform(get("/api/projects/" + projectName + "/search")
				.param("q", " a ").header("Authorization", "Bearer " + token));
		refused.andExpect(status().isBadRequest());
		assertThat(json(refused).get("message").asText()).isEqualTo("Search for at least 2 characters.");

		mockMvc.perform(get("/api/projects/" + projectName + "/search")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/projects/" + projectName + "/search").param("q", "ab")
				.param("type", "Position").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/projects/" + projectName + "/search").param("q", "ab")
				.param("limit", "1001").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	/** AC5: another project's matches never appear. */
	@Test
	void anotherProjectsMatchesNeverAppear() throws Exception {
		String other = newProject("project-search-other-" + ts);
		ok(command("EditGoal", Map.of("projectName", other, "name", "Other " + word,
				"text", "")));
		ok(command("EditGoal", Map.of("projectName", projectName, "name", "Mine " + word,
				"text", "")));

		assertThat(names(search(word, null, null, null).get("results")))
				.containsExactly("Mine " + word);
	}

	/** Steps (by their stored name), actors, glossary terms and stakeholders are searched too. */
	@Test
	void searchesStepsActorsTermsAndStakeholders() throws Exception {
		ok(command("EditActor", Map.of("projectName", projectName, "name", "Operator " + word)));
		ok(command("EditGlossaryTerm", Map.of("projectName", projectName, "name",
				"Term " + ts, "text", "A " + word + " is a thing.")));
		ok(command("EditNonUserStakeholder", Map.of("projectName", projectName, "name",
				"Vendor " + ts, "text", "Supplies the " + word + ".")));
		Map<String, Object> step = new LinkedHashMap<>();
		step.put("name", "checks the " + word);
		step.put("isScenario", false);
		Map<String, Object> scenario = new LinkedHashMap<>();
		scenario.put("projectName", projectName);
		scenario.put("name", "Check out " + ts);
		scenario.put("text", "");
		scenario.put("scenarioTypeName", "Primary");
		scenario.put("steps", new ArrayList<>(List.of(step)));
		ok(command("EditScenario", scenario));

		JsonNode page = search(word, null, null, null);

		assertThat(types(page.get("results")))
				.containsExactlyInAnyOrder("Actor", "Step", "GlossaryTerm", "Stakeholder");
		assertThat(page.get("countsByType").get("Step").asInt()).isEqualTo(1);
		assertThat(page.get("countsByType").has("Scenario")).isFalse();
	}

	/** The type filter keeps one type; offset and limit page; total and counts stay complete. */
	@Test
	void filtersByTypeAndPagesWithCompleteCounts() throws Exception {
		for (int i = 0; i < 5; i++) {
			ok(command("EditGoal", Map.of("projectName", projectName, "name",
					"Goal " + i + " " + word, "text", "")));
		}
		ok(command("EditStory", Map.of("projectName", projectName, "name", "Story " + word,
				"text", "", "storyTypeName", "Success")));

		JsonNode first = search(word, null, 0, 2);
		assertThat(first.get("total").asInt()).isEqualTo(6);
		assertThat(first.get("countsByType").get("Goal").asInt()).isEqualTo(5);
		assertThat(first.get("countsByType").get("Story").asInt()).isEqualTo(1);
		assertThat(first.get("results")).hasSize(2);
		assertThat(first.get("offset").asInt()).isZero();
		assertThat(first.get("limit").asInt()).isEqualTo(2);

		JsonNode last = search(word, null, 4, 2);
		assertThat(types(last.get("results"))).containsExactly("Goal", "Story");

		JsonNode stories = search(word, "story", null, null);
		assertThat(stories.get("total").asInt()).isEqualTo(1);
		assertThat(types(stories.get("results"))).containsExactly("Story");
	}

	/** The gateway (MCP and the CLI read through it) answers the same results as a list. */
	@Test
	void theGatewayAnswersAList() throws Exception {
		ok(command("EditGoal", Map.of("projectName", projectName, "name", "Gate " + word,
				"text", "")));
		ok(command("EditStory", Map.of("projectName", projectName, "name", "Story " + ts,
				"text", word, "storyTypeName", "Success")));

		List<ProjectSearchResultDto> all = asAdmin(
				() -> queryGateway.searchProjectEntities(projectName, word));
		List<ProjectSearchResultDto> page = asAdmin(
				() -> queryGateway.searchProjectEntities(projectName, word, "Story", 0, 10));

		assertThat(all).extracting(ProjectSearchResultDto::entityType)
				.containsExactly("Goal", "Story");
		assertThat(page).extracting(ProjectSearchResultDto::entityType).containsExactly("Story");
	}

	// ---- helpers --------------------------------------------------------------------------------

	private static <T> T asAdmin(Supplier<T> call) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", "x", List.of()));
		try {
			return call.get();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private String newProject(String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand projectCommand = getProjectCommandFactory().newEditProjectCommand();
		projectCommand.setEditedBy(admin);
		projectCommand.setName(name);
		projectCommand.setText("project search test");
		projectCommand.setOrganizationName("ProjectSearchOrg-" + name);
		getCommandHandler().execute(projectCommand);
		return name;
	}

	private long issueOnGoal(long goalId, String text) throws Exception {
		return id(ok(command("EditIssue", Map.of("projectName", projectName, "entityType",
				"Goal", "entityId", goalId, "text", text, "mustBeResolved", false,
				"severity", "LOW"))));
	}

	private JsonNode search(String q, String type, Integer offset, Integer limit)
			throws Exception {
		var request = get("/api/projects/" + projectName + "/search").param("q", q)
				.header("Authorization", "Bearer " + token);
		if (type != null) {
			request.param("type", type);
		}
		if (offset != null) {
			request.param("offset", offset.toString());
		}
		if (limit != null) {
			request.param("limit", limit.toString());
		}
		return ok(mockMvc.perform(request));
	}

	private static List<String> types(JsonNode results) {
		List<String> types = new ArrayList<>();
		results.forEach(r -> types.add(r.get("entityType").asText()));
		return types;
	}

	private static List<String> names(JsonNode results) {
		List<String> names = new ArrayList<>();
		results.forEach(r -> names.add(r.get("name").asText()));
		return names;
	}

	private static long id(JsonNode entity) {
		return entity.get("id").asLong();
	}

	private ResultActions command(String type, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private JsonNode json(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
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
