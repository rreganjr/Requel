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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.service.auth.JwtService;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #445, end to end over HTTP: a login JWT acts as its account as the account is now, not as it
 * was when the token was issued.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginTokenAccountCheckIT extends AbstractIntegrationTestCase {

    private static final String PASSWORD = "Token-Check-123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${requel.jwt.secret:}")
    private String jwtSecret;

    @Test
    public void aRenamedUsersTokenActsAsTheRenamedAccountAndStaysUnprivileged() throws Exception {
        String username = "tok-rename-" + System.nanoTime();
        createUser(username, "ProjectUserRole");
        String token = login(username);

        jdbcTemplate.update("update users set username = ? where username = ?", username + "-new", username);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username + "-new"));

        String newAdmin = "tok-made-admin-" + System.nanoTime();
        mockMvc.perform(post("/api/commands/EditUser")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", newAdmin, "password", PASSWORD, "repassword", PASSWORD,
                                "name", newAdmin, "emailAddress", newAdmin + "@example.com",
                                "organizationName", "TokenCheckOrg",
                                "userRoleNames", List.of("SystemAdminUserRole", "ProjectUserRole")))))
                .andExpect(status().isForbidden());
        assertThrows(NoSuchUserException.class, () -> getUserRepository().findUserByUsername(newAdmin),
                "no user is created");
    }

    @Test
    public void aDemotedAdminLosesAdminReadsAtOnce() throws Exception {
        String username = "tok-demote-" + System.nanoTime();
        User user = createUser(username, "SystemAdminUserRole", "ProjectUserRole");
        String token = login(username);
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand demote = getUserCommandFactory().newEditUserCommand();
        demote.setEditedBy(admin);
        demote.setUser(getUserRepository().findUserByUsername(username));
        demote.setUsername(username);
        demote.setName(username);
        demote.setEmailAddress(username + "@example.com");
        demote.setOrganizationName("TokenCheckOrg");
        demote.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(demote);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /** A token issued before 2.0.3 carries no user id and is refused, even for admin. */
    @Test
    public void aTokenWithoutAUserIdIsRefused() throws Exception {
        String secret = jwtSecret.isBlank() ? JwtService.BUILT_IN_SECRET : jwtSecret;
        String token = Jwts.builder()
                .subject("admin")
                .claim("roles", List.of("SystemAdminUserRole", "ProjectUserRole"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private User createUser(String username, String... roles) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword(PASSWORD);
        cmd.setRepassword(PASSWORD);
        cmd.setName(username);
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("TokenCheckOrg");
        for (String role : roles) {
            cmd.addUserRoleName(role);
        }
        return getCommandHandler().execute(cmd).getUser();
    }
}
