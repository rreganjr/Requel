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
package com.rreganjr.requel.tagging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Tag reads check project read access (issue #447), through the real security chain.
 *
 * <p>
 * Two projects, each with a goal and a story carrying a project tag and a shared global tag.
 * Three callers: a stakeholder on project A, a user on neither project, and the system
 * administrator. A named project the caller can't read is 403; an entity or tag in one is 404, the
 * same as one that doesn't exist; a global tag lists only the entities the caller can read.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TagQueryAccessIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TagRepository tagRepository;

	private String projectA;
	private String projectB;
	private Long goalA;
	private Long storyA;
	private Long goalB;
	private Long storyB;
	private Long tagA;
	private Long tagB;
	private Long globalTag;
	private String globalValue;

	private String memberToken;
	private String outsiderToken;
	private String adminToken;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		projectA = "tag-access-a-" + ts;
		projectB = "tag-access-b-" + ts;
		Project a = newProject(admin, projectA);
		Project b = newProject(admin, projectB);
		goalA = idOf(newGoal(admin, a, "Goal A " + ts));
		storyA = idOf(newStory(admin, a, "Story A " + ts));
		goalB = idOf(newGoal(admin, b, "Goal B " + ts));
		storyB = idOf(newStory(admin, b, "Story B " + ts));

		String member = "tag-member-" + ts;
		String outsider = "tag-outsider-" + ts;
		createUser(member);
		createUser(outsider);
		addStakeholder(a, member);

		adminToken = login("admin", "admin");
		memberToken = login(member, member);
		outsiderToken = login(outsider, outsider);

		globalValue = "shared-" + ts;
		tagA = newTag(projectA, idOf(a), "area", "a-" + ts);
		tagB = newTag(projectB, idOf(b), "area", "b-" + ts);
		globalTag = newTag(null, null, "area", globalValue);
		for (Long goal : List.of(goalA, goalB)) {
			assign(tagFor(goal), "Goal", goal);
			assign(globalTag, "Goal", goal);
		}
		for (Long story : List.of(storyA, storyB)) {
			assign(tagFor(story), "Story", story);
			assign(globalTag, "Story", story);
		}
	}

	private Long tagFor(Long entityId) {
		return (entityId.equals(goalA) || entityId.equals(storyA)) ? tagA : tagB;
	}

	@Test
	void anOutsiderIsRefusedProjectBsTagsAndCategories() throws Exception {
		for (String route : List.of("/api/tags", "/api/tags/categories", "/api/tags/categories/typed")) {
			read(outsiderToken, route + "?projectName=" + projectB).andExpect(status().isForbidden());
			read(memberToken, route + "?projectName=" + projectB).andExpect(status().isForbidden());
		}
	}

	@Test
	void anEntityInAProjectTheCallerCantReadIsNotFound() throws Exception {
		read(outsiderToken, "/api/tags/on-entity?entityType=Goal&entityId=" + goalB)
				.andExpect(status().isNotFound());
		read(memberToken, "/api/tags/on-entity?entityType=Story&entityId=" + storyB)
				.andExpect(status().isNotFound());
		// the same answer as for an entity that doesn't exist
		read(outsiderToken, "/api/tags/on-entity?entityType=Goal&entityId=" + Long.MAX_VALUE)
				.andExpect(status().isNotFound());
	}

	@Test
	void aProjectTagTheCallerCantReadIsNotFound() throws Exception {
		read(outsiderToken, "/api/tags/" + tagB + "/entities").andExpect(status().isNotFound());
		read(memberToken, "/api/tags/" + tagB + "/entities").andExpect(status().isNotFound());
		read(outsiderToken, "/api/tags/" + Long.MAX_VALUE + "/entities")
				.andExpect(status().isNotFound());
	}

	@Test
	void aMemberReadsItsOwnProjectsTags() throws Exception {
		assertThat(values(json(read(memberToken, "/api/tags?projectName=" + projectA))))
				.contains(valueOf(tagA), globalValue).doesNotContain(valueOf(tagB));
		assertThat(strings(json(read(memberToken, "/api/tags/categories?projectName=" + projectA))))
				.contains("area");
		read(memberToken, "/api/tags/categories/typed?projectName=" + projectA)
				.andExpect(status().isOk());
		assertThat(values(json(read(memberToken, "/api/tags/on-entity?entityType=Goal&entityId="
				+ goalA)))).containsExactlyInAnyOrder(valueOf(tagA), globalValue);
		assertThat(refs(json(read(memberToken, "/api/tags/" + tagA + "/entities"))))
				.containsExactlyInAnyOrder("Goal:" + goalA, "Story:" + storyA);
	}

	@Test
	void aGlobalTagListsOnlyTheEntitiesTheCallerCanRead() throws Exception {
		String route = "/api/tags/" + globalTag + "/entities";
		assertThat(refs(json(read(memberToken, route))))
				.containsExactlyInAnyOrder("Goal:" + goalA, "Story:" + storyA);
		assertThat(refs(json(read(outsiderToken, route)))).isEmpty();
		assertThat(refs(json(read(adminToken, route)))).containsExactlyInAnyOrder(
				"Goal:" + goalA, "Story:" + storyA, "Goal:" + goalB, "Story:" + storyB);
	}

	@Test
	void theAdministratorReadsEveryProject() throws Exception {
		assertThat(values(json(read(adminToken, "/api/tags?projectName=" + projectB))))
				.contains(valueOf(tagB), globalValue);
		assertThat(values(json(read(adminToken, "/api/tags/on-entity?entityType=Story&entityId="
				+ storyB)))).containsExactlyInAnyOrder(valueOf(tagB), globalValue);
		assertThat(refs(json(read(adminToken, "/api/tags/" + tagB + "/entities"))))
				.containsExactlyInAnyOrder("Goal:" + goalB, "Story:" + storyB);
	}

	@Test
	void withNoProjectEveryCallerGetsGlobalTagsOnly() throws Exception {
		for (String token : List.of(outsiderToken, memberToken, adminToken)) {
			assertThat(values(json(read(token, "/api/tags")))).contains(globalValue)
					.doesNotContain(valueOf(tagA), valueOf(tagB));
			read(token, "/api/tags/categories").andExpect(status().isOk());
			read(token, "/api/tags/categories/typed").andExpect(status().isOk());
		}
	}

	@Test
	void aMissingProjectIsNotFoundAndAnUnknownTypeIsABadRequest() throws Exception {
		read(memberToken, "/api/tags?projectName=no-such-project-" + System.nanoTime())
				.andExpect(status().isNotFound());
		read(memberToken, "/api/tags/on-entity?entityType=NoSuchType&entityId=" + goalA)
				.andExpect(status().isBadRequest());
	}

	// --- fixture helpers ---

	private String valueOf(Long tagId) {
		return tagRepository.findTagById(tagId).getValue();
	}

	private Project newProject(User admin, String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setName(name);
		command.setText("tag access test");
		command.setOrganizationName("TagAccessOrg-" + name);
		return getCommandHandler().execute(command).getProject();
	}

	private Goal newGoal(User admin, Project project, String name) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(admin);
		command.setGoalContainer(project);
		command.setName(name);
		command.setText("goal text");
		return getCommandHandler().execute(command).getGoal();
	}

	private Story newStory(User admin, Project project, String name) throws Exception {
		EditStoryCommand command = getProjectCommandFactory().newEditStoryCommand();
		command.setEditedBy(admin);
		command.setStoryContainer(project);
		command.setName(name);
		command.setText("story text");
		command.setStoryTypeName("Success");
		return getCommandHandler().execute(command).getStory();
	}

	private static Long idOf(Object entity) {
		Object real = Hibernate.unproxy(entity);
		if (real instanceof ProjectImpl project) {
			return project.getId();
		}
		if (real instanceof GoalImpl goal) {
			return goal.getId();
		}
		return ((StoryImpl) real).getId();
	}

	private void createUser(String username) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand command = getUserCommandFactory().newEditUserCommand();
		command.setEditedBy(admin);
		command.setUsername(username);
		command.setPassword(username);
		command.setRepassword(username);
		command.setName(username);
		command.setEmailAddress(username + "@example.com");
		command.setPhoneNumber("");
		command.setOrganizationName("TagAccessOrg");
		command.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(command);
	}

	private void addStakeholder(Project project, String username) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserStakeholderCommand command = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		command.setEditedBy(admin);
		command.setProjectOrDomain(project);
		command.setUsername(username);
		command.setStakeholderPermissions(Set.of(StakeholderPermissionImpl
				.generatePermissionKey(Goal.class, StakeholderPermissionType.Edit)));
		getCommandHandler().execute(command);
	}

	private Long newTag(String projectName, Long projectId, String category, String value)
			throws Exception {
		Map<String, Object> input = new java.util.HashMap<>();
		if (projectName != null) {
			input.put("projectName", projectName);
		}
		input.put("category", category);
		input.put("value", value);
		command("EditTag", input).andExpect(status().isOk());
		return tagRepository.findTag(projectId, category, value).getId();
	}

	private void assign(Long tagId, String entityType, Long entityId) throws Exception {
		command("AssignTag", Map.of("tagId", tagId, "entityType", entityType, "entityId", entityId))
				.andExpect(status().isOk());
	}

	private ResultActions command(String type, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", username,
						"password", password))))
				.andExpect(status().isOk()).andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token")
				.asText();
	}

	private ResultActions read(String token, String route) throws Exception {
		return mockMvc.perform(get(route).header("Authorization", "Bearer " + token));
	}

	private JsonNode json(ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString());
	}

	private static List<String> values(JsonNode tags) {
		List<String> values = new ArrayList<>();
		tags.forEach(t -> values.add(t.path("value").asText()));
		return values;
	}

	private static List<String> strings(JsonNode array) {
		List<String> values = new ArrayList<>();
		array.forEach(v -> values.add(v.asText()));
		return values;
	}

	private static List<String> refs(JsonNode refs) {
		List<String> values = new ArrayList<>();
		refs.forEach(r -> values.add(r.path("entityType").asText() + ":" + r.path("entityId").asLong()));
		return values;
	}
}
