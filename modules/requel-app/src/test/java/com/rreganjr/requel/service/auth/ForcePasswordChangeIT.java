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
package com.rreganjr.requel.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #381: a user who must change their password can sign in, see who they are and change the
 * password, and nothing else - over a login JWT, a personal access token or an MCP OAuth token.
 * Each test makes its own users, so the order doesn't matter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class ForcePasswordChangeIT extends AbstractIntegrationTestCase {

	private static final String PASSWORD = "first-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JWKSource<SecurityContext> jwkSource;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private String adminJwt;
	private int counter;

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
		adminJwt = login("admin", "admin").get("token").asText();
	}

	@Test
	void aFlaggedUserSignsInAndIsToldToChangeTheirPassword() throws Exception {
		String username = newUser(true);
		JsonNode login = login(username, PASSWORD);
		assertThat(login.get("user").get("mustChangePassword").asBoolean()).isTrue();

		String jwt = login.get("token").asText();
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.mustChangePassword").value(true));
	}

	@Test
	void aFlaggedLoginTokenCanDoNothingElse() throws Exception {
		String jwt = login(newUser(true), PASSWORD).get("token").asText();
		assertRestricted(mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + jwt)));
		assertRestricted(mockMvc.perform(post("/api/commands/EditProject")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"nope\"}")));
		assertRestricted(mockMvc.perform(post("/api/auth/tokens")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"nope\"}")));
		assertRestricted(mockMvc.perform(get("/api/gateway/query/projects")
				.header("Authorization", "Bearer " + jwt)));
		assertRestricted(mockMvc.perform(post("/api/mcp")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON).content("{}")));
	}

	@Test
	void anApiTokenIssuedBeforeTheFlagIsRestrictedOnceItIsSet() throws Exception {
		String username = newUser(false);
		grantManageApiTokens(username);
		String jwt = login(username, PASSWORD).get("token").asText();
		String pat = mintToken(jwt);
		mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk());

		setFlag(username, true);

		assertRestricted(mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + pat)));
		assertRestricted(mockMvc.perform(get("/api/gateway/query/projects")
				.header("Authorization", "Bearer " + pat)));
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk());
	}

	@Test
	void anOAuthTokenForAFlaggedUserIsRestricted() throws Exception {
		String token = oauthToken(newUser(true));
		assertRestricted(mockMvc.perform(get("/api/gateway/query/projects")
				.header("Authorization", "Bearer " + token)));
		assertRestricted(mockMvc.perform(post("/api/mcp")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{}")));
	}

	@Test
	void aWrongCurrentPasswordIsRefused() throws Exception {
		String jwt = login(newUser(true), PASSWORD).get("token").asText();
		changePassword(jwt, "not-it", "second-secret", "second-secret")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.violations[0].field").value("currentPassword"));
	}

	@Test
	void theSamePasswordIsRefused() throws Exception {
		String jwt = login(newUser(true), PASSWORD).get("token").asText();
		changePassword(jwt, PASSWORD, PASSWORD, PASSWORD)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.violations[0].field").value("newPassword"));
	}

	@Test
	void aMismatchedConfirmationIsRefused() throws Exception {
		String username = newUser(true);
		String jwt = login(username, PASSWORD).get("token").asText();
		changePassword(jwt, PASSWORD, "second-secret", "something-else")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.violations[0].field").value("confirmPassword"));
		// Nothing changed: the old password still signs in, still flagged.
		assertThat(login(username, PASSWORD).get("user").get("mustChangePassword").asBoolean()).isTrue();
	}

	@Test
	void changingThePasswordClearsTheFlagAndReturnsAWorkingToken() throws Exception {
		String username = newUser(true);
		String jwt = login(username, PASSWORD).get("token").asText();
		MvcResult result = changePassword(jwt, PASSWORD, "second-secret", "second-secret")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.mustChangePassword").value(false))
				.andReturn();
		String fresh = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

		mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + fresh))
				.andExpect(status().isOk());
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isFalse();
		JsonNode again = login(username, "second-secret");
		assertThat(again.get("user").get("mustChangePassword").asBoolean()).isFalse();
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void anAdminCanSetAndClearTheFlag() throws Exception {
		String username = newUser(false);
		editUserOverRest(adminJwt, username, true).andExpect(status().isOk());
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isTrue();
		editUserOverRest(adminJwt, username, false).andExpect(status().isOk());
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isFalse();
	}

	@Test
	void aUserCannotSetTheFlagOnTheirOwnAccount() throws Exception {
		String username = newUser(false);
		String jwt = login(username, PASSWORD).get("token").asText();
		editUserOverRest(jwt, username, true).andExpect(status().isOk());
		assertThat(getUserRepository().findUserByUsername(username).isMustChangePassword()).isFalse();
	}

	// ---- helpers ------------------------------------------------------------------------------

	private void assertRestricted(ResultActions actions) throws Exception {
		actions.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value(PasswordChangeRequiredFilter.AUTHORITY));
	}

	private ResultActions changePassword(String jwt, String current, String next, String confirm)
			throws Exception {
		return mockMvc.perform(post("/api/auth/password")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"currentPassword", current, "newPassword", next, "confirmPassword", confirm))));
	}

	private ResultActions editUserOverRest(String jwt, String username, boolean flag) throws Exception {
		User user = getUserRepository().findUserByUsername(username);
		Map<String, Object> input = new HashMap<>();
		input.put("version", user.getVersion());
		input.put("username", username);
		input.put("name", user.getName());
		input.put("emailAddress", user.getEmailAddress());
		input.put("organizationName", user.getOrganization().getName());
		input.put("mustChangePassword", flag);
		return mockMvc.perform(post("/api/commands/EditUser")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(input)));
	}

	private JsonNode login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								Map.of("username", username, "password", password))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String mintToken(String jwt) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/tokens")
						.header("Authorization", "Bearer " + jwt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"381\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	private String oauthToken(String username) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("http://localhost")
				.subject(username)
				.audience(List.of("requel-cli"))
				.claim("scope", "mcp")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(300))
				.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return new NimbusJwtEncoder(jwkSource).encode(JwtEncoderParameters.from(header, claims))
				.getTokenValue();
	}

	private String newUser(boolean mustChangePassword) throws Exception {
		String username = "pwc-" + System.currentTimeMillis() + "-" + (++counter);
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword(PASSWORD);
		cmd.setRepassword(PASSWORD);
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("PwcTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		cmd.setMustChangePassword(mustChangePassword);
		getCommandHandler().execute(cmd);
		return username;
	}

	private void setFlag(String username, boolean flag) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			User user = getUserRepository().findUserByUsername(username);
			user.setMustChangePassword(flag);
			getUserRepository().merge(user);
		});
	}
}
