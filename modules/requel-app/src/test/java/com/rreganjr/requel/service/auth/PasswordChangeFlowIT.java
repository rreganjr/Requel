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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Password changes need the current password, and a change ends older sign-ins (issue #446).
 *
 * <ul>
 * <li>{@code EditUser} on your own account refuses a password, whether the caller holds a login
 * token or a personal access token.</li>
 * <li>Your own change through {@code /api/auth/password} returns a token that works; a login
 * token from before the change gets 401; your personal access tokens are kept; your OAuth
 * authorizations are deleted.</li>
 * <li>An admin reset of another user ends that user's login tokens and revokes their personal
 * access tokens.</li>
 * </ul>
 *
 * Login tokens carry {@code iat} to the second, so each test waits past a second boundary before a
 * change, the way a real old token is older.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/db/oauth2-schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PasswordChangeFlowIT extends AbstractIntegrationTestCase {

    private static final String NEW_PASSWORD = "Another-pass-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String prefix;

    @BeforeAll
    void setUpFixture() throws Exception {
        initializeBaselineData();
        prefix = "pwflow-" + System.currentTimeMillis() + "-";
    }

    @Test
    void editUserOnYourOwnAccountRefusesAPassword() throws Exception {
        String username = newUser("self-jwt");
        String token = login(username, username);

        editUser(token, ownEdit(username, NEW_PASSWORD))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].field").value("password"));
        // the password is unchanged: the old one still signs in, the new one doesn't
        login(username, username);
        loginStatus(username, NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void editUserOnYourOwnAccountRefusesAPasswordWithAPersonalAccessTokenToo() throws Exception {
        String username = newUser("self-pat");
        String pat = personalAccessToken(username);

        editUser(pat, ownEdit(username, NEW_PASSWORD))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].field").value("password"));
        login(username, username);
    }

    @Test
    void editUserOnYourOwnAccountStillSavesTheOtherFields() throws Exception {
        String username = newUser("self-name");
        String token = login(username, username);
        Map<String, Object> edit = ownEdit(username, null);
        edit.put("name", "Renamed");
        editUser(token, edit).andExpect(status().isOk());
        assertThat(getUserRepository().findUserByUsername(username).getName()).isEqualTo("Renamed");
    }

    @Test
    void yourOwnChangeEndsOlderSignInsButKeepsYourAccessTokens() throws Exception {
        String username = newUser("own-change");
        String oldToken = login(username, username);
        String pat = personalAccessToken(username);
        insertOAuthAuthorization(username);
        nextSecond();

        MvcResult changed = mockMvc.perform(post("/api/auth/password")
                .header("Authorization", "Bearer " + oldToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("currentPassword", username,
                        "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        String newToken = objectMapper.readTree(changed.getResponse().getContentAsString())
                .get("token").asText();

        me(newToken).andExpect(status().isOk());
        me(oldToken).andExpect(status().isUnauthorized());
        me(pat).andExpect(status().isOk());
        assertThat(oauthAuthorizations(username)).isZero();
    }

    @Test
    void anAdminResetEndsTheUsersSignInsAndRevokesTheirAccessTokens() throws Exception {
        String username = newUser("admin-reset");
        String oldToken = login(username, username);
        String pat = personalAccessToken(username);
        insertOAuthAuthorization(username);
        String adminToken = login("admin", "admin");
        nextSecond();

        Map<String, Object> reset = ownEdit(username, NEW_PASSWORD);
        editUser(adminToken, reset).andExpect(status().isOk());

        me(oldToken).andExpect(status().isUnauthorized());
        me(pat).andExpect(status().isUnauthorized());
        assertThat(oauthAuthorizations(username)).isZero();
        me(login(username, NEW_PASSWORD)).andExpect(status().isOk());
        // the admin's own session is untouched
        me(adminToken).andExpect(status().isOk());
    }

    // --- helpers ---

    /** Wait until the wall clock passes the next whole second. */
    private static void nextSecond() throws InterruptedException {
        long now = System.currentTimeMillis();
        Thread.sleep(1000 - (now % 1000) + 50);
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token));
    }

    private ResultActions editUser(String token, Map<String, Object> input) throws Exception {
        return mockMvc.perform(post("/api/commands/EditUser")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)));
    }

    /** The account's current fields, with a password when {@code password} isn't null. */
    private Map<String, Object> ownEdit(String username, String password) {
        User user = getUserRepository().findUserByUsername(username);
        Map<String, Object> input = new HashMap<>();
        input.put("username", username);
        input.put("name", user.getName());
        input.put("emailAddress", user.getEmailAddress());
        input.put("phoneNumber", "");
        input.put("organizationName", "PwFlowOrg");
        if (password != null) {
            input.put("password", password);
            input.put("repassword", password);
        }
        return input;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = loginStatus(username, password).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private ResultActions loginStatus(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", username,
                        "password", password))));
    }

    private String personalAccessToken(String username) {
        User user = getUserRepository().findUserByUsername(username);
        return apiTokenService.create(user.getId(), "pwflow", null).plaintext();
    }

    private void insertOAuthAuthorization(String username) {
        jdbcTemplate.update("INSERT INTO oauth2_authorization (id, registered_client_id, principal_name,"
                + " authorization_grant_type) VALUES (?, ?, ?, ?)", UUID.randomUUID().toString(),
                "pwflow-client", username, "authorization_code");
        assertThat(oauthAuthorizations(username)).isEqualTo(1);
    }

    private int oauthAuthorizations(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE principal_name = ?", Integer.class,
                username);
        return count == null ? 0 : count;
    }

    /** A project user whose password is its username. */
    private String newUser(String name) throws Exception {
        String username = prefix + name;
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand command = getUserCommandFactory().newEditUserCommand();
        command.setEditedBy(admin);
        command.setUsername(username);
        command.setPassword(username);
        command.setRepassword(username);
        command.setName(username);
        command.setEmailAddress(username + "@example.com");
        command.setPhoneNumber("");
        command.setOrganizationName("PwFlowOrg");
        command.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(command);
        return username;
    }
}
