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
package com.rreganjr.requel.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.service.api.dto.LoginRequest;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link AuthController}.
 *
 * Security rules:
 * - POST /api/auth/login is {@code permitAll()} — no auth token needed.
 * - GET  /api/auth/me   is {@code authenticated()} — covered by class-level @WithMockUser.
 *
 * Collaborators mocked:
 * - {@code UserRepository} — user lookup
 * - {@code JwtService}     — token generation
 * - {@code UserDtoMapper}  — entity → DTO conversion
 * - {@code CurrentUserResolver} — principal → domain User for /me
 *
 * Scenarios covered:
 * - login: valid credentials → 200 with token and UserDto
 * - login: wrong password → 401 with UNAUTHORIZED error body
 * - login: unknown username → 401 (exception caught, same body)
 * - login: error body contains expected fields (error code, message)
 * - me: authenticated user → 200 with UserDto
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserRepository userRepository;
    @MockBean JwtService jwtService;
    @MockBean UserDtoMapper userDtoMapper;
    @MockBean CurrentUserResolver currentUserResolver;

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    @Test
    @WithAnonymousUser
    void loginWithValidCredentialsReturnsTokenAndUserDto() throws Exception {
        User user = mock(User.class);
        when(user.isPassword("secret")).thenReturn(true);
        when(userRepository.findUserByUsername("alice")).thenReturn(user);
        when(userDtoMapper.getRoleStrings(user)).thenReturn(List.of("ProjectUserRole"));
        when(userDtoMapper.getPermissionStrings(user)).thenReturn(List.of("createProject"));
        when(jwtService.generateToken(eq(user), any(), any())).thenReturn("jwt-token-abc");
        when(userDtoMapper.toDto(user)).thenReturn(new UserDto(
                1L, "alice", "Alice", null, null, null,
                List.of("ProjectUserRole"), List.of("createProject"),
                Map.of("ProjectUserRole", List.of("createProject")), 0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-abc"))
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.roles[0]").value("ProjectUserRole"));
    }

    @Test
    @WithAnonymousUser
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        User user = mock(User.class);
        when(user.isPassword("wrong")).thenReturn(false);
        when(userRepository.findUserByUsername("alice")).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @WithAnonymousUser
    void loginWithUnknownUsernameReturnsUnauthorized() throws Exception {
        when(userRepository.findUserByUsername("ghost"))
                .thenThrow(NoSuchUserException.forUsername("ghost"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ghost", "anything"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void meReturnsCurrentUserDto() throws Exception {
        User user = mock(User.class);
        when(currentUserResolver.resolve()).thenReturn(user);
        when(userDtoMapper.toDto(user)).thenReturn(new UserDto(
                7L, "bob", "Bob", "bob@example.com", null, "ACME",
                List.of("SystemAdminUserRole"), List.of(),
                Map.of("SystemAdminUserRole", List.of()), 2));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.emailAddress").value("bob@example.com"))
                .andExpect(jsonPath("$.organizationName").value("ACME"));
    }

    @Test
    @WithAnonymousUser
    void meReturnsUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
