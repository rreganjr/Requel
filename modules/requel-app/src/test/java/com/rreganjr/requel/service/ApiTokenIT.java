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
package com.rreganjr.requel.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end integration test for personal access tokens (issue #73, Slices 1-3 together): mint a
 * PAT via {@code /api/auth/tokens}, use it as a bearer to reach a protected endpoint, confirm
 * revocation takes effect on the next request, and that token management is strictly own-tokens-only.
 * Exercises the REST API + the JwtAuthenticationFilter PAT branch as a real client would.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class ApiTokenIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String adminJwt;
	private String otherUsername;
	private String otherJwt;
	private String noPermUsername;
	private String noPermJwt;

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();

		long ts = System.currentTimeMillis();
		otherUsername = "tok-other-" + ts;
		createUser(otherUsername, "secret");
		// A project user with NO manageApiTokens permission (opt-in default, #85).
		noPermUsername = "tok-noperm-" + ts;
		createUser(noPermUsername, "secret");

		// manageApiTokens is per-user and not granted by default (#85). Grant it to the users that
		// manage tokens in these tests (admin + otherUser); noPermUser is deliberately left without.
		grantManageApiTokens("admin");
		grantManageApiTokens(otherUsername);

		// Log in AFTER granting so the JWTs carry the up-to-date permission claims.
		adminJwt = login("admin", "admin");
		otherJwt = login(otherUsername, "secret");
		noPermJwt = login(noPermUsername, "secret");
	}

	@Test
	void mintedTokenAuthenticatesAndIsListed() throws Exception {
		String[] minted = mintToken(adminJwt, "ci");
		String pat = minted[0];

		// The PAT authenticates a protected endpoint as the owning user.
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"));

		// And the token is listed for its owner.
		mockMvc.perform(get("/api/auth/tokens").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name=='ci')]").exists());
	}

	@Test
	void revocationTakesEffectImmediately() throws Exception {
		String[] minted = mintToken(adminJwt, "to-revoke");
		String pat = minted[0];
		String id = minted[1];

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/auth/tokens/" + id).header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isNoContent());

		// Next request with the revoked token is unauthenticated.
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pat))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void revokeIsOwnTokensOnly() throws Exception {
		String[] minted = mintToken(adminJwt, "admins-token");
		String id = minted[1];

		// Another user cannot revoke admin's token (404, not revealing it exists).
		mockMvc.perform(delete("/api/auth/tokens/" + id).header("Authorization", "Bearer " + otherJwt))
				.andExpect(status().isNotFound());

		// Admin's token still works.
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + minted[0]))
				.andExpect(status().isOk());
	}

	@Test
	void revokedTokenCanBeHardDeleted() throws Exception {
		String[] minted = mintToken(adminJwt, "to-delete");
		String id = minted[1];

		// Revoke first, then hard-delete the row.
		mockMvc.perform(delete("/api/auth/tokens/" + id).header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/auth/tokens/" + id + "/permanent")
						.header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isNoContent());

		// It is gone from the owner's list.
		mockMvc.perform(get("/api/auth/tokens").header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id==" + id + ")]").doesNotExist());
	}

	@Test
	void activeTokenCannotBeHardDeleted() throws Exception {
		String[] minted = mintToken(adminJwt, "still-active");
		String pat = minted[0];
		String id = minted[1];

		// Deleting an active token is rejected; it must be revoked first.
		mockMvc.perform(delete("/api/auth/tokens/" + id + "/permanent")
						.header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isConflict());

		// The token still works.
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pat))
				.andExpect(status().isOk());
	}

	@Test
	void hardDeleteIsOwnTokensOnly() throws Exception {
		String[] minted = mintToken(adminJwt, "admins-deletable");
		String id = minted[1];

		// Revoke it so it would otherwise be deletable.
		mockMvc.perform(delete("/api/auth/tokens/" + id).header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isNoContent());

		// Another user cannot delete admin's token (404, not revealing it exists).
		mockMvc.perform(delete("/api/auth/tokens/" + id + "/permanent")
						.header("Authorization", "Bearer " + otherJwt))
				.andExpect(status().isNotFound());

		// Admin's token row is untouched (still listed, as REVOKED).
		mockMvc.perform(get("/api/auth/tokens").header("Authorization", "Bearer " + adminJwt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id==" + id + ")].status").value("REVOKED"));
	}

	@Test
	void unauthenticatedCreateIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"nope\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void blankNameIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.header("Authorization", "Bearer " + adminJwt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void projectUserWithoutManagePermissionCannotCreate() throws Exception {
		// noPermUser holds ProjectUserRole but was not granted manageApiTokens (#85): 403, not 201.
		mockMvc.perform(post("/api/auth/tokens")
						.header("Authorization", "Bearer " + noPermJwt)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("name", "nope"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void projectUserWithoutManagePermissionCannotList() throws Exception {
		mockMvc.perform(get("/api/auth/tokens").header("Authorization", "Bearer " + noPermJwt))
				.andExpect(status().isForbidden());
	}

	@Test
	void projectUserWithManagePermissionCanListAndCreate() throws Exception {
		// otherUser holds ProjectUserRole and WAS granted manageApiTokens: management succeeds.
		mockMvc.perform(get("/api/auth/tokens").header("Authorization", "Bearer " + otherJwt))
				.andExpect(status().isOk());
		String[] minted = mintToken(otherJwt, "other-ok");
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + minted[0]))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(otherUsername));
	}

	// ---- helpers ------------------------------------------------------------------------------

	/** @return {plaintextToken, tokenId} */
	private String[] mintToken(String jwt, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/tokens")
						.header("Authorization", "Bearer " + jwt)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("name", name))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.startsWith("reqpat_")))
				.andExpect(jsonPath("$.tokenInfo.status").value("ACTIVE"))
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return new String[] {body.get("token").asText(), body.get("tokenInfo").get("id").asText()};
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								Map.of("username", username, "password", password))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	private void createUser(String username, String password) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(admin);
		cmd.setUsername(username);
		cmd.setPassword(password);
		cmd.setRepassword(password);
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("TokTestOrg");
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}
}
