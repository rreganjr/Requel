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
package com.rreganjr.requel.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #449: who can reach the Actuator endpoints. Health is anonymous and shows its details
 * only to a system admin; info and metrics need a system admin, by login JWT or personal access
 * token; prometheus isn't exposed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManagementEndpointSecurityIT extends AbstractIntegrationTestCase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ObjectProvider<BuildProperties> buildProperties;

	private String adminJwt;
	private String adminPat;
	private String userJwt;

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
		String user = "mgmt-user-" + System.currentTimeMillis();
		createUser(user);
		grantManageApiTokens("admin");
		adminJwt = login("admin", "admin");
		userJwt = login(user, user);
		adminPat = mintToken(adminJwt);
	}

	// ---- health -----------------------------------------------------------------------------

	@Test
	void healthIsOpenWithoutDetails() throws Exception {
		JsonNode body = json(mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andReturn());
		assertThat(body.path("status").asText()).isEqualTo("UP");
		assertThat(body.has("components")).isFalse();
	}

	@Test
	void healthHidesDetailsFromANonAdmin() throws Exception {
		JsonNode body = json(mockMvc.perform(bearer(get("/actuator/health"), userJwt))
				.andExpect(status().isOk()).andReturn());
		assertThat(body.has("components")).isFalse();
	}

	@Test
	void healthShowsDetailsToAnAdmin() throws Exception {
		JsonNode body = json(mockMvc.perform(bearer(get("/actuator/health"), adminJwt))
				.andExpect(status().isOk()).andReturn());
		assertThat(body.path("components").has("db")).isTrue();
	}

	// ---- info -------------------------------------------------------------------------------

	@Test
	void infoNeedsSignIn() throws Exception {
		mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
	}

	@Test
	void infoIsRefusedToANonAdmin() throws Exception {
		mockMvc.perform(bearer(get("/actuator/info"), userJwt)).andExpect(status().isForbidden());
	}

	@Test
	void infoShowsBuildJavaAndOsToAnAdmin() throws Exception {
		JsonNode body = json(mockMvc.perform(bearer(get("/actuator/info"), adminJwt))
				.andExpect(status().isOk()).andReturn());
		assertThat(body.has("java")).isTrue();
		assertThat(body.has("os")).isTrue();
		BuildProperties build = buildProperties.getIfAvailable();
		if (build != null) {
			assertThat(body.path("build").path("version").asText()).isEqualTo(build.getVersion());
		}
	}

	// ---- metrics ----------------------------------------------------------------------------

	@Test
	void metricsNeedSignIn() throws Exception {
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/metrics/jvm.memory.used")).andExpect(status().isUnauthorized());
	}

	@Test
	void metricsAreRefusedToANonAdmin() throws Exception {
		mockMvc.perform(bearer(get("/actuator/metrics"), userJwt)).andExpect(status().isForbidden());
	}

	@Test
	void metricsAreOpenToAnAdminByJwtAndByPat() throws Exception {
		JsonNode byJwt = json(mockMvc.perform(bearer(get("/actuator/metrics"), adminJwt))
				.andExpect(status().isOk()).andReturn());
		assertThat(byJwt.path("names").isArray()).isTrue();
		mockMvc.perform(bearer(get("/actuator/metrics/jvm.memory.used"), adminPat))
				.andExpect(status().isOk());
	}

	// ---- prometheus -------------------------------------------------------------------------

	/**
	 * Not exposed, so no chain or endpoint claims it and no scrape output comes back. (MockMvc
	 * reports the {@code SpaController} fall-through as a 200 with an empty body, so the check is
	 * on the content, not the status.)
	 */
	@Test
	void prometheusIsNotServed() throws Exception {
		String body = mockMvc.perform(bearer(get("/actuator/prometheus"), adminJwt)).andReturn()
				.getResponse().getContentAsString();
		assertThat(body).doesNotContain("# TYPE").doesNotContain("jvm_memory");
	}

	// ---- helpers ----------------------------------------------------------------------------

	private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request,
			String token) {
		return request.header("Authorization", "Bearer " + token);
	}

	private JsonNode json(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", username,
						"password", password))))
				.andExpect(status().isOk()).andReturn();
		return json(result).get("token").asText();
	}

	private String mintToken(String jwt) throws Exception {
		MvcResult result = mockMvc.perform(bearer(post("/api/auth/tokens"), jwt)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", "mgmt-it"))))
				.andExpect(status().isCreated()).andReturn();
		return json(result).get("token").asText();
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
		command.setOrganizationName("MgmtOrg");
		command.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(command);
	}
}
