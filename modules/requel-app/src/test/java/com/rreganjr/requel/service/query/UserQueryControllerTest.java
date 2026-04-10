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
package com.rreganjr.requel.service.query;

import com.rreganjr.requel.service.api.dto.OrganizationDto;
import com.rreganjr.requel.service.api.dto.RoleDto;
import com.rreganjr.requel.service.api.dto.UserDto;
import com.rreganjr.requel.service.auth.UserDtoMapper;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.UserSet;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link UserQueryController}.
 *
 * Uses {@code @SpringBootTest(webEnvironment=MOCK)} for the same reason as
 * {@link ProjectQueryControllerTest}: the XML component scan pulls JPA-dependent
 * beans into every Spring context, making {@code @WebMvcTest} unusable here.
 *
 * Collaborators mocked:
 * - {@code UserRepository} — all data access
 * - {@code UserDtoMapper} — user entity → DTO conversion (covers its own unit tests)
 *
 * Scenarios covered:
 * - listUsers: sorted by username, empty list
 * - getUser: 200 with DTO, 404 when not found
 * - listOrganizations: sorted by name, empty list
 * - listRoles: sorted by displayName with permissions sorted by name
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "SystemAdminUserRole")
class UserQueryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserRepository userRepository;
    @MockBean UserDtoMapper userDtoMapper;

    // -------------------------------------------------------------------------
    // listUsers
    // -------------------------------------------------------------------------

    @Test
    void listUsersReturnsSortedByUsername() throws Exception {
        UserSet users = mock(UserSet.class);
        User alice = mock(User.class);
        User zara  = mock(User.class);
        User bob   = mock(User.class);
        when(users.stream()).thenReturn(java.util.stream.Stream.of(zara, alice, bob));
        when(userRepository.findUsers()).thenReturn(users);

        when(userDtoMapper.toDto(alice)).thenReturn(new UserDto(1L, "alice", "Alice",
                null, null, null, List.of(), List.of(), Map.of(), 0));
        when(userDtoMapper.toDto(zara)).thenReturn(new UserDto(2L, "zara", "Zara",
                null, null, null, List.of(), List.of(), Map.of(), 0));
        when(userDtoMapper.toDto(bob)).thenReturn(new UserDto(3L, "bob", "Bob",
                null, null, null, List.of(), List.of(), Map.of(), 0));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"))
                .andExpect(jsonPath("$[2].username").value("zara"));
    }

    @Test
    void listUsersReturnsEmptyListWhenNoUsers() throws Exception {
        UserSet users = mock(UserSet.class);
        when(users.stream()).thenReturn(java.util.stream.Stream.of());
        when(userRepository.findUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // getUser
    // -------------------------------------------------------------------------

    @Test
    void getUserReturnsDto() throws Exception {
        User user = mock(User.class);
        when(userRepository.findUserByUsername("alice")).thenReturn(user);
        when(userDtoMapper.toDto(user)).thenReturn(new UserDto(
                42L, "alice", "Alice Smith",
                "alice@example.com", "555-1234", "ACME",
                List.of("ProjectUserRole"), List.of("createProject"),
                Map.of("ProjectUserRole", List.of("createProject")), 3));

        mockMvc.perform(get("/api/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.emailAddress").value("alice@example.com"))
                .andExpect(jsonPath("$.organizationName").value("ACME"))
                .andExpect(jsonPath("$.roles[0]").value("ProjectUserRole"));
    }

    @Test
    void getUserReturns404WhenNotFound() throws Exception {
        when(userRepository.findUserByUsername("ghost"))
                .thenThrow(NoSuchUserException.forUsername("ghost"));

        mockMvc.perform(get("/api/users/ghost"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // listOrganizations
    // -------------------------------------------------------------------------

    @Test
    void listOrganizationsReturnsSortedByName() throws Exception {
        Organization acme  = stubOrg(1L, "ACME");
        Organization zeta  = stubOrg(2L, "Zeta Corp");
        Organization beta  = stubOrg(3L, "Beta Inc");
        when(userRepository.findOrganizations()).thenReturn(Set.of(zeta, acme, beta));

        mockMvc.perform(get("/api/users/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("ACME"))
                .andExpect(jsonPath("$[1].name").value("Beta Inc"))
                .andExpect(jsonPath("$[2].name").value("Zeta Corp"));
    }

    @Test
    void listOrganizationsReturnsEmptyList() throws Exception {
        when(userRepository.findOrganizations()).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/users/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // listRoles
    // -------------------------------------------------------------------------

    @Test
    void listRolesReturnsSortedByDisplayNameWithPermissionsSortedByName() throws Exception {
        // Two role classes; controller derives displayName by stripping "UserRole" suffix
        // and inserting spaces before capitals: ProjectUserRole → "Project", SystemAdminUserRole → "System Admin"
        @SuppressWarnings("unchecked")
        Class<? extends UserRole> projectRole = (Class<? extends UserRole>)
                com.rreganjr.requel.project.ProjectUserRole.class;
        @SuppressWarnings("unchecked")
        Class<? extends UserRole> adminRole = (Class<? extends UserRole>)
                com.rreganjr.requel.user.impl.SystemAdminUserRole.class;

        when(userRepository.findUserRoleTypes()).thenReturn(Set.of(projectRole, adminRole));

        UserRolePermission createProject = stubPermission("createProject");
        UserRolePermission addStakeholder = stubPermission("addStakeholder");
        when(userRepository.findUserRolePermissions(projectRole))
                .thenReturn(Set.of(addStakeholder, createProject));
        when(userRepository.findUserRolePermissions(adminRole))
                .thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // sorted by displayName: "Project" < "System Admin"
                .andExpect(jsonPath("$[0].roleName").value("ProjectUserRole"))
                .andExpect(jsonPath("$[0].availablePermissions.length()").value(2))
                // permissions sorted by name: addStakeholder < createProject
                .andExpect(jsonPath("$[0].availablePermissions[0].name").value("addStakeholder"))
                .andExpect(jsonPath("$[0].availablePermissions[1].name").value("createProject"))
                .andExpect(jsonPath("$[1].roleName").value("SystemAdminUserRole"))
                .andExpect(jsonPath("$[1].availablePermissions.length()").value(0));
    }

    @Test
    void listRolesReturnsEmptyList() throws Exception {
        when(userRepository.findUserRoleTypes()).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Organization stubOrg(Long id, String name) {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(id);
        when(org.getName()).thenReturn(name);
        return org;
    }

    private UserRolePermission stubPermission(String name) {
        UserRolePermission perm = mock(UserRolePermission.class);
        when(perm.getName()).thenReturn(name);
        return perm;
    }
}
