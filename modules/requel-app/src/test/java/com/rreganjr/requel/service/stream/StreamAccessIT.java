/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #450: the event stream's session and subscription rules against the real security chain,
 * database and access check. A session belongs to the user who opened it; a subscription needs
 * read access to the target's project, except the two broadcasts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamAccessIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StreamSessionStore sessionStore;

	private String member;
	private String memberToken;
	private String outsiderToken;
	private String adminToken;
	private Long memberProjectId;
	private Long otherProjectId;
	private Long memberGoal;
	private Long otherGoal;
	private long ts;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		ts = System.currentTimeMillis();
		Project mine = newProject(admin, "stream-access-a-" + ts);
		Project theirs = newProject(admin, "stream-access-b-" + ts);
		memberProjectId = idOf(mine);
		otherProjectId = idOf(theirs);
		memberGoal = idOf(newGoal(admin, mine, "Stream goal A " + ts));
		otherGoal = idOf(newGoal(admin, theirs, "Stream goal B " + ts));

		member = "stream-member-" + ts;
		String outsider = "stream-outsider-" + ts;
		createUser(member);
		createUser(outsider);
		addStakeholder(mine, member);

		memberToken = login(member, member);
		outsiderToken = login(outsider, outsider);
		adminToken = login("admin", "admin");
	}

	// ---- reconnect ----------------------------------------------------------------------------

	@Test
	void anotherUserCannotReconnectToASession() throws Exception {
		String sessionId = "member-session-" + ts;
		sessionStore.createSession(sessionId, member);
		sessionStore.addSubscription(sessionId, "Goal:" + memberGoal);

		mockMvc.perform(get("/api/events/stream").param("sessionId", sessionId)
				.header("Authorization", "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());

		assertThat(sessionStore.getSessionOwner(sessionId)).isEqualTo(member);
		assertThat(sessionStore.getSubscriptions(sessionId)).containsExactly("Goal:" + memberGoal);
	}

	@Test
	void theOwnerCanReconnectToTheirSession() throws Exception {
		String sessionId = "member-own-session-" + ts;
		sessionStore.createSession(sessionId, member);

		mockMvc.perform(get("/api/events/stream").param("sessionId", sessionId)
				.header("Authorization", "Bearer " + memberToken))
				.andExpect(request().asyncStarted());

		assertThat(sessionStore.getSessionOwner(sessionId)).isEqualTo(member);
	}

	@Test
	void unreadableInitialSubscriptionsAreSkipped() throws Exception {
		mockMvc.perform(get("/api/events/stream")
				.param("subscribe", "Project:0", "Goal:" + memberGoal, "Goal:" + otherGoal)
				.header("Authorization", "Bearer " + memberToken))
				.andExpect(request().asyncStarted());

		assertThat(sessionStore.getSessionsForTarget("Goal:" + otherGoal)).isEmpty();
		assertThat(sessionStore.getSessionsForTarget("Goal:" + memberGoal)).isNotEmpty();
	}

	// ---- subscribe ----------------------------------------------------------------------------

	@Test
	void aMemberSubscribesToTheirProjectsGoal() throws Exception {
		subscribe(memberToken, member, "Goal", memberGoal).andExpect(status().isOk());
	}

	@Test
	void aNonMemberCannotSubscribeToAnotherProjectsEntities() throws Exception {
		subscribe(memberToken, member, "Goal", otherGoal).andExpect(status().isForbidden());
		subscribe(memberToken, member, "Project", otherProjectId).andExpect(status().isForbidden());
	}

	@Test
	void aMemberSubscribesToTheirProject() throws Exception {
		subscribe(memberToken, member, "Project", memberProjectId).andExpect(status().isOk());
	}

	@Test
	void theBroadcastsAreOpenToAnyoneSignedIn() throws Exception {
		subscribe(memberToken, member, "Project", 0L).andExpect(status().isOk());
		subscribe(memberToken, member, "Permissions", 0L).andExpect(status().isOk());
	}

	@Test
	void anUnknownTypeAndAMissingEntityGetTheSameRefusal() throws Exception {
		subscribe(memberToken, member, "Banana", 1L).andExpect(status().isForbidden());
		subscribe(memberToken, member, "Goal", Long.MAX_VALUE).andExpect(status().isForbidden());
		subscribe(memberToken, member, "User", 1L).andExpect(status().isForbidden());
	}

	@Test
	void anAdminSubscribesToAnyProjectsEntities() throws Exception {
		subscribe(adminToken, "admin", "Goal", otherGoal).andExpect(status().isOk());
	}

	// ---- helpers ------------------------------------------------------------------------------

	private ResultActions subscribe(String token, String owner, String type, Long id) throws Exception {
		String sessionId = "sub-" + owner + "-" + type + "-" + id + "-" + System.nanoTime();
		sessionStore.createSession(sessionId, owner);
		return mockMvc.perform(post("/api/events/stream/subscriptions")
				.header("Authorization", "Bearer " + token)
				.header("X-Session-Id", sessionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("targetType", type, "targetId", id))));
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
		command.setText("stream access test");
		command.setOrganizationName("StreamAccessOrg-" + name);
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
		command.setOrganizationName("StreamAccessOrg");
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

	private static Long idOf(Object entity) {
		Object real = Hibernate.unproxy(entity);
		if (real instanceof ProjectImpl project) {
			return project.getId();
		}
		return ((GoalImpl) real).getId();
	}
}
