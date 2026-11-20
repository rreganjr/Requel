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
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.ServerInfoDto;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Issue #449: the {@code getServerInfo} gateway read, which tells MCP clients and the CLI which
 * build of Requel they're talking to. Any signed-in caller may read it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerInfoIT extends AbstractIntegrationTestCase {

	private static final String ROUTE = "/api/gateway/query/server-info";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private QueryGateway queryGateway;

	@Autowired
	private ObjectProvider<BuildProperties> buildProperties;

	@Autowired
	private ObjectProvider<GitProperties> gitProperties;

	private String userJwt;

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
		String user = "server-info-" + System.currentTimeMillis();
		createUser(user);
		userJwt = login(user, user);
	}

	@Test
	void needsSignIn() throws Exception {
		mockMvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
	}

	@Test
	void anySignedInUserReadsTheBuild() throws Exception {
		JsonNode body = objectMapper.readTree(mockMvc.perform(get(ROUTE)
				.header("Authorization", "Bearer " + userJwt))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		ServerInfoDto expected = expected();
		assertThat(body.path("version").asText()).isEqualTo(expected.version());
		assertThat(body.path("commit").isNull() ? null : body.path("commit").asText())
				.isEqualTo(expected.commit());
	}

	/** The commit time must parse: an unreadable format in git.properties leaves it null. */
	@Test
	void aBuildFromACheckoutReportsTheCommitTime() {
		if (gitProperties.getIfAvailable() != null) {
			assertThat(queryGateway.getServerInfo().commitTime()).isNotNull();
		}
	}

	@Test
	void theInProcessGatewayAgrees() {
		assertThat(queryGateway.getServerInfo()).isEqualTo(expected());
	}

	/** What the build files say, or the documented fallbacks when a file is absent. */
	private ServerInfoDto expected() {
		BuildProperties build = buildProperties.getIfAvailable();
		GitProperties git = gitProperties.getIfAvailable();
		return new ServerInfoDto(
				build != null ? build.getVersion() : "unknown",
				git != null ? git.getShortCommitId() : null,
				git != null && git.getCommitTime() != null ? git.getCommitTime().toString() : null,
				build != null && build.getTime() != null ? build.getTime().toString() : null);
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", username,
						"password", password))))
				.andExpect(status().isOk()).andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
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
		command.setOrganizationName("ServerInfoOrg");
		command.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(command);
	}
}
