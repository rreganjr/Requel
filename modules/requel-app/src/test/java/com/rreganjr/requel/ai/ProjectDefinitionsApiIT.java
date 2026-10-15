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
package com.rreganjr.requel.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinition;
import com.rreganjr.requel.assistant.core.definition.AssistantDefinitionStore;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #264 over HTTP: a project member with AssistantDefinition[Edit] creates, edits, forks,
 * reverts and deletes the project's definitions through the command endpoint and reads them back;
 * a broken rule is a 422 naming the field, a stale version a 409, and without the permission
 * both reads and writes are a 403. Existing Project[Edit] holders get the permission on upgrade.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=noop",
		"spring.ai.model.chat=none" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectDefinitionsApiIT extends AbstractIntegrationTestCase {

	private static final String BUNDLED_GOAL = "ai-review-goal";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AssistantDefinitionStore store;

	@Autowired
	private StakeholderPermissionsInitializer permissionsInitializer;

	private String projectName;
	private String authorToken;
	private String goalEditorToken;
	private String noAccessToken;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		projectName = "defs-" + ts;
		Project project = newProject(admin, projectName);
		String author = "def-author-" + ts;
		String goalEditor = "def-goal-editor-" + ts;
		String noAccess = "def-noaccess-" + ts;
		createUser(author);
		createUser(goalEditor);
		createUser(noAccess);
		addStakeholder(project, author, Set.of(key(AssistantDefinitionClass.TYPE),
				key(Project.class)));
		// Goal[Edit] alone: neither the definitions permission nor Project[Edit], so the
		// upgrade backfill (which follows Project[Edit]) never reaches this persona
		addStakeholder(project, goalEditor, Set.of(key(Goal.class)));
		authorToken = login(author);
		goalEditorToken = login(goalEditor);
		noAccessToken = login(noAccess);
	}

	/** The marker's class, under a name that doesn't clash with the definition record's. */
	private static final class AssistantDefinitionClass {
		static final Class<?> TYPE = com.rreganjr.requel.project.AssistantDefinition.class;
	}

	@Test
	void withoutThePermissionReadsAndWritesAreForbidden() throws Exception {
		for (String token : List.of(goalEditorToken, noAccessToken)) {
			mockMvc.perform(get("/api/projects/" + projectName + "/definitions")
					.header("Authorization", "Bearer " + token))
					.andExpect(status().isForbidden());
			mockMvc.perform(get("/api/projects/" + projectName + "/definitions/" + BUNDLED_GOAL)
					.header("Authorization", "Bearer " + token))
					.andExpect(status().isForbidden());
			command(token, "CreateAssistantDefinition", policy("denied"))
					.andExpect(status().isForbidden());
			command(token, "ForkAssistantDefinition", Map.of("projectName", projectName,
					"key", BUNDLED_GOAL)).andExpect(status().isForbidden());
		}
	}

	@Test
	void aProjectPolicyIsCreatedEditedAndDeleted() throws Exception {
		JsonNode created = result(command(authorToken, "CreateAssistantDefinition",
				policy("house-style")).andExpect(status().isOk()));
		assertThat(created.path("source").asText()).isEqualTo("PROJECT");
		assertThat(created.path("taskType").asText()).isEqualTo("POLICY_REVIEW");
		int lock = created.path("lockVersion").asInt();

		mockMvc.perform(get("/api/projects/" + projectName + "/definitions")
				.header("Authorization", "Bearer " + authorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.key == 'house-style')].enabled").value(true));
		// its own switch, under its own name
		mockMvc.perform(get("/api/projects/" + projectName + "/assistants")
				.header("Authorization", "Bearer " + authorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.assistantId == 'house-style')].displayName")
						.value("House style"));

		Map<String, Object> edit = new HashMap<>(policy("house-style"));
		edit.put("version", lock + 5);
		edit.put("instructions", "Use the glossary's terms.");
		command(authorToken, "EditAssistantDefinition", edit).andExpect(status().isConflict());
		edit.put("version", lock);
		JsonNode edited = result(command(authorToken, "EditAssistantDefinition", edit)
				.andExpect(status().isOk()));
		assertThat(edited.path("version").asInt()).isEqualTo(2);
		assertThat(edited.path("instructions").asText()).isEqualTo("Use the glossary's terms.");

		command(authorToken, "DeleteAssistantDefinition", Map.of("projectName", projectName,
				"key", "house-style", "version", edited.path("lockVersion").asInt()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/projects/" + projectName + "/definitions/house-style")
				.header("Authorization", "Bearer " + authorToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void aBrokenRuleIsRefusedOnItsField() throws Exception {
		Map<String, Object> bad = new HashMap<>(policy("Not A Key"));
		bad.put("contextProviders", List.of("telepathy"));
		bad.put("executorBean", "commandHandler");
		bad.put("instructions", "x".repeat(200_000));
		bad.put("vocabulary", List.of());

		JsonNode body = objectMapper.readTree(command(authorToken, "CreateAssistantDefinition",
				bad).andExpect(status().isUnprocessableEntity()).andReturn().getResponse()
				.getContentAsString());

		List<String> fields = body.path("violations").findValuesAsText("field");
		assertThat(fields).contains("key", "contextProviders", "executorBean", "instructions",
				"vocabulary");
		// a bundled key is customized by forking, not created
		command(authorToken, "CreateAssistantDefinition", Map.of("projectName", projectName,
				"kind", "REVIEW", "key", BUNDLED_GOAL, "displayName", "x", "contextProviders",
				List.of("entity"), "instructions", "x", "vocabulary", List.of(Map.of("type", "A",
						"description", "a")))).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void aForkRecordsItsOriginAndRevertBringsTheBundledOneBack() throws Exception {
		AssistantDefinition bundled = store.bundled().stream()
				.filter(d -> d.key().equals(BUNDLED_GOAL)).findFirst().orElseThrow();

		JsonNode fork = result(command(authorToken, "ForkAssistantDefinition", Map.of(
				"projectName", projectName, "key", BUNDLED_GOAL)).andExpect(status().isOk()));
		assertThat(fork.path("forkedFromVersion").asInt()).isEqualTo(bundled.version());
		assertThat(fork.path("bundledVersion").asInt()).isEqualTo(bundled.version());

		MvcResult detail = mockMvc.perform(get("/api/projects/" + projectName + "/definitions/"
				+ BUNDLED_GOAL).header("Authorization", "Bearer " + authorToken))
				.andExpect(status().isOk()).andReturn();
		JsonNode both = objectMapper.readTree(detail.getResponse().getContentAsString());
		assertThat(both.path("definition").path("source").asText()).isEqualTo("PROJECT");
		assertThat(both.path("bundled").path("instructions").asText())
				.isEqualTo(bundled.instructions());

		command(authorToken, "RevertAssistantDefinition", Map.of("projectName", projectName,
				"key", BUNDLED_GOAL, "version", fork.path("lockVersion").asInt()))
				.andExpect(status().isOk());
		Long projectId = getProjectRepository().findProjectByName(projectName).getId();
		assertThat(store.definitionsFor(projectId, bundled.taskType()))
				.filteredOn(d -> d.key().equals(BUNDLED_GOAL)).containsExactly(bundled);
	}

	@Test
	void projectEditorsGetThePermissionOnUpgrade() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		Project project = newProject(admin, "defs-upgrade-" + ts);
		String owner = "def-owner-" + ts;
		createUser(owner);
		addStakeholder(project, owner, Set.of(key(Project.class)));

		permissionsInitializer.initialize();

		UserStakeholder stakeholder = getProjectRepository().findStakeholderByProjectOrDomainAndUser(
				getProjectRepository().findProjectByName("defs-upgrade-" + ts),
				getUserRepository().findUserByUsername(owner));
		assertThat(stakeholder.getStakeholderPermissions()).extracting(p -> p.getPermissionKey())
				.contains(key(AssistantDefinitionClass.TYPE));
	}

	private Map<String, Object> policy(String key) {
		Map<String, Object> input = new HashMap<>();
		input.put("projectName", projectName);
		input.put("kind", "POLICY");
		input.put("key", key);
		input.put("displayName", "House style");
		input.put("scope", List.of());
		input.put("contextProviders", List.of("entity"));
		input.put("instructions", "Goals name a measurable outcome.\n\n{{vocabulary}}");
		input.put("vocabulary", List.of(Map.of("type", "RULE_BROKEN", "description",
				"the rule is broken")));
		input.put("localOnly", false);
		return input;
	}

	private ResultActions command(String token, String type, Map<String, Object> input)
			throws Exception {
		return mockMvc.perform(post("/api/commands/" + type)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private JsonNode result(ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString())
				.path("entity");
	}

	private static String key(Class<?> type) {
		return StakeholderPermissionImpl.generatePermissionKey(type,
				StakeholderPermissionType.Edit);
	}

	private Project newProject(User admin, String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setName(name);
		command.setText("project definitions test");
		command.setOrganizationName("DefsOrg-" + name);
		return getCommandHandler().execute(command).getProject();
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
		command.setOrganizationName("DefsOrg");
		command.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(command);
	}

	private void addStakeholder(Project project, String username, Set<String> permissionKeys)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserStakeholderCommand command = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		command.setEditedBy(admin);
		command.setProjectOrDomain(project);
		command.setUsername(username);
		command.setStakeholderPermissions(permissionKeys);
		getCommandHandler().execute(command);
	}

	private String login(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", username,
						"password", username))))
				.andExpect(status().isOk()).andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token")
				.asText();
	}
}
