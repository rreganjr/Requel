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
package com.rreganjr.requel.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #387: the AI review through the gateway (what the MCP tools call) and over HTTP - the
 * editor's endpoints on the {@code /api/**} chain and the CLI's on {@code /api/gateway/**}. Same
 * context as {@link ProjectDefinitionsApiIT} (AI on, the noop provider). Refusing a request when
 * gateway writes are off is {@link GatewayAiReviewControllerTest}'s.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"requel.ai.enabled=true",
		"requel.ai.provider=noop",
		"spring.ai.model.chat=none" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AiReviewGatewayIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AiReviewGateway gateway;

	@Autowired
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired
	private JWKSource<SecurityContext> jwkSource;

	private Project project;
	private String projectName;
	private String member;
	private String outsider;
	private Long goalId;

	@BeforeAll
	void setUpFixture() throws Exception {
		initializeBaselineData();
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		projectName = "ai-review-gw-" + ts;
		project = newProject(admin, projectName);
		member = "air-member-" + ts;
		outsider = "air-outsider-" + ts;
		createUser(member);
		createUser(outsider);
		// No Goal[Edit] or Annotation[Edit]: a review needs project access only.
		addStakeholder(project, member, Set.of());
		goalId = newGoal(admin, project, "Gateway review " + ts).getId();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- in process, as the MCP tools call it ---------------------------------------------------

	@Test
	void aStakeholderRequestsAndFollowsAReview() throws Exception {
		authenticateAs(member);
		AiReviewRequestDto runs = gateway.requestReview(projectName, "Goal", goalId);
		assertThat(runs.runId()).isNotBlank();

		AiReviewDto latest = gateway.latestReview(projectName, "Goal", goalId, null).orElseThrow();
		assertThat(latest.runId()).isEqualTo(runs.runId());
		assertThat(latest.status()).isIn("QUEUED", "RUNNING", "SUCCEEDED");
		String again = gateway.requestReview(projectName, "Goal", goalId).runId();
		if (!latest.finished()) {
			assertThat(again).as("still in flight: the same run").isEqualTo(runs.runId());
		}
	}

	@Test
	void theGatewayRefusesWithTheRightKinds() {
		authenticateAs(outsider);
		assertKind(() -> gateway.requestReview(projectName, "Goal", goalId),
				GatewayException.Kind.UNAUTHORIZED);

		authenticateAs(member);
		assertKind(() -> gateway.requestReview("no-such-project-387", "Goal", goalId),
				GatewayException.Kind.NOT_FOUND);
		assertKind(() -> gateway.requestReview(projectName, "Goal", 987654321L),
				GatewayException.Kind.NOT_FOUND);
		assertKind(() -> gateway.requestReview(projectName, "Report", goalId),
				GatewayException.Kind.INVALID_INPUT);
		assertKind(() -> gateway.latestReview(projectName, "Goal", goalId, "CORPUS_REVIEW"),
				GatewayException.Kind.INVALID_INPUT);
	}

	@Test
	void anEntityFromAnotherProjectIsRefused() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		Project other = newProject(admin, "ai-review-gw-other-" + System.nanoTime());
		Long otherGoal = newGoal(admin, other, "Elsewhere " + System.nanoTime()).getId();
		authenticateAs("admin");
		assertKind(() -> gateway.requestReview(projectName, "Goal", otherGoal),
				GatewayException.Kind.INVALID_INPUT);
	}

	@Test
	void aTypeSwitchedOffInTheProjectIsAConflictNamingIt() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		Project off = newProject(admin, "ai-review-gw-off-" + System.nanoTime());
		Long offGoal = newGoal(admin, off, "Off " + System.nanoTime()).getId();
		settingsStore.setEnabled(off.getId(), "ai-review-goal", false, admin);
		authenticateAs("admin");

		assertThatThrownBy(() -> gateway.requestReview(off.getName(), "Goal", offGoal))
				.isInstanceOfSatisfying(GatewayException.class, e -> {
					assertThat(e.getKind()).isEqualTo(GatewayException.Kind.CONFLICT);
					assertThat(e.getMessage())
							.isEqualTo("AI review of goals is switched off for this project.");
				});
	}

	// ---- over HTTP ------------------------------------------------------------------------------

	@Test
	void theEditorsEndpointsSayItIsAvailableAndReturnTheRun() throws Exception {
		String token = login(member);
		mockMvc.perform(get("/api/ai/reviews/availability").param("entityType", "Goal")
				.param("entityId", goalId.toString()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(true));
		mockMvc.perform(post("/api/ai/reviews").param("entityType", "Goal")
				.param("entityId", goalId.toString()).header("Authorization", "Bearer " + token))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.runId").isNotEmpty());
	}

	@Test
	void theCliEndpointsRequestAndReadWithAnOAuthToken() throws Exception {
		String token = oauthToken(member);
		MvcResult requested = mockMvc.perform(post("/api/gateway/ai/reviews/" + projectName)
				.param("entityType", "Goal").param("entityId", goalId.toString())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isAccepted()).andReturn();
		String runId = objectMapper.readTree(requested.getResponse().getContentAsString())
				.get("runId").asText();
		mockMvc.perform(get("/api/gateway/ai/reviews/" + projectName).param("entityType", "Goal")
				.param("entityId", goalId.toString()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.runId").value(runId));
		mockMvc.perform(post("/api/gateway/ai/reviews/" + projectName).param("entityType", "Goal")
				.param("entityId", goalId.toString()).header("Authorization", "Bearer "
						+ oauthToken(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.kind").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/gateway/ai/reviews/" + projectName).param("entityType", "Goal")
				.param("entityId", goalId.toString()))
				.andExpect(status().isUnauthorized());
	}

	// ---- helpers --------------------------------------------------------------------------------

	private interface GatewayCall {
		Object call() throws GatewayException;
	}

	private static void assertKind(GatewayCall call, GatewayException.Kind kind) {
		assertThatThrownBy(call::call).isInstanceOfSatisfying(GatewayException.class,
				e -> assertThat(e.getKind()).isEqualTo(kind));
	}

	private static void authenticateAs(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, null, List.of()));
	}

	private String oauthToken(String username) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("http://localhost").subject(username)
				.audience(List.of("requel-cli")).claim("scope", "mcp").issuedAt(now)
				.expiresAt(now.plusSeconds(300)).build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return new NimbusJwtEncoder(jwkSource).encode(JwtEncoderParameters.from(header, claims))
				.getTokenValue();
	}

	private Project newProject(User admin, String name) throws Exception {
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setName(name);
		command.setText("AI review gateway test");
		command.setOrganizationName("AirOrg-" + name);
		return getCommandHandler().execute(command).getProject();
	}

	private Goal newGoal(User admin, Project project, String name) throws Exception {
		EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
		command.setEditedBy(admin);
		command.setGoalContainer(project);
		command.setName(name);
		command.setText("Users can log in.");
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
		command.setOrganizationName("AirOrg");
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
