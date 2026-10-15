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
package com.rreganjr.requel.service.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.service.api.dto.UserPreferencesDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link UserPreferencesController}.
 *
 * GET /api/user-preferences  — returns current prefs (creates defaults on first access).
 * PUT /api/user-preferences  — updates prefs; skips fields with sentinel values
 *                              (limit ≤ 0 means "don't change", null staleness means "don't change").
 *
 * {@code UserPreferences} is a plain JPA entity with a public constructor, so tests
 * use real instances rather than mocks — the actual field mutations are verified via
 * the response JSON.
 *
 * Collaborators mocked:
 * - {@code UserPreferencesRepository} — JPA persistence
 * - {@code CurrentUserResolver}       — resolves current username from security context
 *
 * Scenarios covered:
 * - GET: existing preferences returned with stored values
 * - GET: no prefs in repository → defaults created and returned
 * - PUT: valid limit and staleness → both updated in response
 * - PUT: limit = 0 (sentinel) → limit not changed; staleness still applied
 * - PUT: staleness = null → staleness not changed; limit still applied
 * - PUT: invalid staleness string → 400 Bad Request
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class UserPreferencesControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserPreferencesRepository repository;
    @MockBean CurrentUserResolver currentUserResolver;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("alice");
        when(currentUserResolver.resolve()).thenReturn(user);

        // Default: save returns whatever was passed in (no DB side-effects needed)
        when(repository.save(any(UserPreferences.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // GET /api/user-preferences
    // -------------------------------------------------------------------------

    @Test
    void getPreferencesReturnsStoredValues() throws Exception {
        UserPreferences prefs = new UserPreferences("alice");
        prefs.setSidebarProjectLimit(5);
        prefs.setSidebarProjectStaleness(SidebarProjectStaleness.SIX_MONTHS);
        when(repository.findByUsername("alice")).thenReturn(Optional.of(prefs));

        mockMvc.perform(get("/api/user-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarProjectLimit").value(5))
                .andExpect(jsonPath("$.sidebarProjectStaleness").value("SIX_MONTHS"));
    }

    @Test
    void getPreferencesCreatesDefaultsWhenNoneExist() throws Exception {
        when(repository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarProjectLimit").value(10))
                .andExpect(jsonPath("$.sidebarProjectStaleness").value("THREE_MONTHS"));

        // Verify a new record was persisted
        verify(repository).save(any(UserPreferences.class));
    }

    // -------------------------------------------------------------------------
    // PUT /api/user-preferences
    // -------------------------------------------------------------------------

    @Test
    void updatePreferencesSetsLimitAndStaleness() throws Exception {
        UserPreferences prefs = new UserPreferences("alice");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(prefs));

        UserPreferencesDto input = new UserPreferencesDto(20, "ONE_MONTH");

        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarProjectLimit").value(20))
                .andExpect(jsonPath("$.sidebarProjectStaleness").value("ONE_MONTH"));
    }

    @Test
    void updatePreferencesWithZeroLimitDoesNotChangeLimit() throws Exception {
        UserPreferences prefs = new UserPreferences("alice");
        prefs.setSidebarProjectLimit(15);
        when(repository.findByUsername("alice")).thenReturn(Optional.of(prefs));

        // limit = 0 is the sentinel meaning "don't update"
        UserPreferencesDto input = new UserPreferencesDto(0, "ALWAYS");

        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarProjectLimit").value(15))
                .andExpect(jsonPath("$.sidebarProjectStaleness").value("ALWAYS"));
    }

    @Test
    void updatePreferencesWithNullStalenessDoesNotChangeStaleness() throws Exception {
        UserPreferences prefs = new UserPreferences("alice");
        prefs.setSidebarProjectStaleness(SidebarProjectStaleness.NINE_MONTHS);
        when(repository.findByUsername("alice")).thenReturn(Optional.of(prefs));

        // null staleness means "don't update"
        UserPreferencesDto input = new UserPreferencesDto(7, null);

        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarProjectLimit").value(7))
                .andExpect(jsonPath("$.sidebarProjectStaleness").value("NINE_MONTHS"));
    }

    @Test
    void updatePreferencesWithInvalidStalenessReturnsBadRequest() throws Exception {
        UserPreferences prefs = new UserPreferences("alice");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(prefs));

        UserPreferencesDto input = new UserPreferencesDto(10, "NOT_A_REAL_VALUE");

        mockMvc.perform(put("/api/user-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }
}
