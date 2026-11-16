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
package com.rreganjr.requel.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.dto.ReplaceTermTextResultDto;
import com.rreganjr.requel.service.gateway.GatewayPolicyConfig;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #348: after a rename, the term's detail lists the referers that still say the old name
 * ({@code synonymUses}), and {@code ReplaceTermText} rewrites it, each referer through its own
 * edit command: one the caller may not edit is refused and the rest are applied.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReplaceTermTextIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CommandGateway gateway;

	private String projectName;
	private Long termId;
	private Long synonymId;
	private Goal goal;
	private Story story;
	private Story quietStory;
	private Story otherStory;
	private String storyEditor;

	@BeforeEach
	void setUp() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.nanoTime();
		projectName = "replace-term-" + ts;
		Project project = newProject(admin, projectName);
		goal = newGoal(admin, project, "Rehearse", "Hold the dry-run event a week before.");
		story = newStory(admin, project, "Dry-run event checklist",
				"Before the dry-run event, test the stream.");
		quietStory = newStory(admin, project, "Launch", "Go live on the day.");
		otherStory = newStory(admin, project, "Unlinked", "The dry-run event, but not a referer.");

		GlossaryTerm term = newTerm(admin, project, "Dry-run event", "A rehearsal before going live.",
				Set.of(goal, story, quietStory));
		termId = term.getId();
		rename(admin, project, term, "Dry-run webinar");
		synonymId = synonymIdNamed(project, "Dry-run event");

		storyEditor = "story-editor-" + ts;
		createUser(storyEditor);
		addStakeholder(project, storyEditor, Set.of(key(Story.class)));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void theTermDetailListsTheReferersThatStillSayTheOldName() throws Exception {
		JsonNode detail = json(mockMvc.perform(get("/api/projects/" + projectName + "/terms/" + termId)
				.header("Authorization", "Bearer " + login("admin", "admin"))));
		assertThat(detail.path("alternateTerms").get(0).path("name").asText()).isEqualTo("Dry-run event");
		JsonNode uses = detail.path("synonymUses");
		assertThat(uses).hasSize(1);
		assertThat(uses.get(0).path("synonymId").asLong()).isEqualTo(synonymId);
		assertThat(names(uses.get(0).path("referers")))
				.containsExactlyInAnyOrder("Rehearse", "Dry-run event checklist");
	}

	@Test
	void aTermWithNoSynonymsHasNoUses() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		Project project = getProjectRepository().findProjectByName(projectName);
		GlossaryTerm plain = newTerm(admin, project, "Rehearsal", "A practice run.", Set.of(goal));
		JsonNode detail = json(mockMvc.perform(get("/api/projects/" + projectName + "/terms/"
				+ plain.getId()).header("Authorization", "Bearer " + login("admin", "admin"))));
		assertThat(detail.path("synonymUses").isArray()).isTrue();
		assertThat(detail.path("synonymUses")).isEmpty();
	}

	@Test
	void aStoryEditorGetsTheStoryChangedAndTheGoalRefused() throws Exception {
		int storyVersion = getProjectRepository().findById(Story.class, story.getId()).getVersion();
		ReplaceTermTextResultDto result = replace(login(storyEditor, storyEditor),
				List.of(ref("Goal", goal.getId()), ref("Story", story.getId()),
						ref("Story", quietStory.getId()), ref("Story", otherStory.getId())));

		assertThat(result.applied()).extracting(r -> r.id()).containsExactly(story.getId());
		assertThat(result.refused()).extracting(r -> r.id()).containsExactly(goal.getId());
		assertThat(result.unchanged()).extracting(r -> r.id())
				.containsExactlyInAnyOrder(quietStory.getId(), otherStory.getId());

		Story changed = getProjectRepository().findById(Story.class, story.getId());
		assertThat(changed.getName()).isEqualTo("Dry-run webinar checklist");
		assertThat(changed.getText()).isEqualTo("Before the dry-run webinar, test the stream.");
		assertThat(changed.getVersion()).as("changed through EditStory").isGreaterThan(storyVersion);
		assertThat(getProjectRepository().findById(Goal.class, goal.getId()).getText())
				.isEqualTo("Hold the dry-run event a week before.");
		assertThat(getProjectRepository().findById(Story.class, otherStory.getId()).getText())
				.as("not a referer, so left alone").isEqualTo("The dry-run event, but not a referer.");
	}

	@Test
	void anAdminReplacesItEverywhere() throws Exception {
		ReplaceTermTextResultDto result = replace(login("admin", "admin"),
				List.of(ref("Goal", goal.getId()), ref("Story", story.getId())));
		assertThat(result.applied()).hasSize(2);
		assertThat(result.refused()).isEmpty();
		assertThat(getProjectRepository().findById(Goal.class, goal.getId()).getText())
				.isEqualTo("Hold the dry-run webinar a week before.");

		JsonNode detail = json(mockMvc.perform(get("/api/projects/" + projectName + "/terms/" + termId)
				.header("Authorization", "Bearer " + login("admin", "admin"))));
		assertThat(detail.path("synonymUses")).isEmpty();
	}

	@Test
	void aSynonymThatIsNotTheTermsIsRefusedAsInvalid() throws Exception {
		command(login("admin", "admin"), Map.of("projectName", projectName, "termId", synonymId,
				"synonymId", termId, "referers", List.of(ref("Story", story.getId()))))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void itIsOnTheGateway() throws Exception {
		assertThat(GatewayPolicyConfig.ALLOWED).contains("ReplaceTermText");
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(storyEditor, "x", List.of()));
		GatewayResult result = gateway.execute(new GatewayRequest("ReplaceTermText",
				Map.of("projectName", projectName, "termId", termId, "synonymId", synonymId,
						"referers", List.of(ref("Story", story.getId())))));
		assertThat(result.result()).isInstanceOf(ReplaceTermTextResultDto.class);
		assertThat(((ReplaceTermTextResultDto) result.result()).applied()).hasSize(1);
	}

	// --- helpers ---

	private static Map<String, Object> ref(String type, Long id) {
		return Map.of("entityType", type, "id", id);
	}

	private ReplaceTermTextResultDto replace(String token, List<Map<String, Object>> referers)
			throws Exception {
		MvcResult result = command(token, Map.of("projectName", projectName, "termId", termId,
				"synonymId", synonymId, "referers", referers)).andExpect(status().isOk()).andReturn();
		return objectMapper.treeToValue(objectMapper.readTree(result.getResponse().getContentAsString())
				.path("entity"), ReplaceTermTextResultDto.class);
	}

	private ResultActions command(String token, Map<String, Object> input) throws Exception {
		return mockMvc.perform(post("/api/commands/ReplaceTermText")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private JsonNode json(ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString());
	}

	private static List<String> names(JsonNode refs) {
		List<String> names = new ArrayList<>();
		refs.forEach(r -> names.add(r.path("name").asText()));
		return names;
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", username,
						"password", password))))
				.andExpect(status().isOk()).andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	private Project newProject(User admin, String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setName(name);
		command.setText("replace term test");
		command.setOrganizationName("ReplaceOrg-" + name);
		return getCommandHandler().execute(command).getProject();
	}

	private Goal newGoal(User admin, Project project, String name, String text) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(admin);
		command.setGoalContainer(project);
		command.setName(name);
		command.setText(text);
		return getCommandHandler().execute(command).getGoal();
	}

	private Story newStory(User admin, Project project, String name, String text) throws Exception {
		EditStoryCommand command = getProjectCommandFactory().newEditStoryCommand();
		command.setEditedBy(admin);
		command.setStoryContainer(project);
		command.setName(name);
		command.setText(text);
		command.setStoryTypeName("Success");
		return getCommandHandler().execute(command).getStory();
	}

	private GlossaryTerm newTerm(User admin, Project project, String name, String text,
			Set<ProjectOrDomainEntity> referers) throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(admin);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		command.setReferers(new HashSet<>(referers));
		return getCommandHandler().execute(command).getGlossaryTerm();
	}

	private void rename(User admin, Project project, GlossaryTerm term, String name) throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(admin);
		command.setProjectOrDomain(project);
		command.setGlossaryTerm(term);
		command.setName(name);
		getCommandHandler().execute(command);
	}

	private Long synonymIdNamed(Project project, String name) {
		Project fresh = getProjectRepository().findProjectByName(project.getName());
		return fresh.getGlossaryTerms().stream().map(Hibernate::unproxy)
				.map(GlossaryTermImpl.class::cast)
				.filter(t -> name.equals(t.getName())).findFirst().orElseThrow().getId();
	}

	private static String key(Class<?> type) {
		return StakeholderPermissionImpl.generatePermissionKey(type, StakeholderPermissionType.Edit);
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
		command.setOrganizationName("ReplaceOrg");
		command.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(command);
	}

	private void addStakeholder(Project project, String username, Set<String> permissionKeys)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserStakeholderCommand command = getProjectCommandFactory().newEditUserStakeholderCommand();
		command.setEditedBy(admin);
		command.setProjectOrDomain(project);
		command.setUsername(username);
		command.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(command);
	}
}
