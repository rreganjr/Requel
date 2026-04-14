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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.*;
import com.rreganjr.requel.project.command.*;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack authorization integration tests.
 *
 * Extends {@link AbstractIntegrationTestCase} for the full Spring context + H2 baseline
 * (admin, project, assistant users; seeded stakeholder permissions). Adds
 * {@code @AutoConfigureMockMvc} to layer MockMvc on top for HTTP dispatch.
 *
 * <h2>TDD note</h2>
 * Tests asserting <b>403</b> will FAIL initially because no project command
 * implements {@link com.rreganjr.platform.command.AuthorizableCommand} yet —
 * the {@code AuthorizingCommandHandler} skips non-authorizable commands, so every
 * permitted and non-permitted user gets 200. Implementing {@code AuthorizableCommand}
 * on each project command is what makes those tests go green.
 *
 * Tests asserting <b>401</b> pass immediately — Spring Security rejects
 * unauthenticated requests before any command code runs.
 *
 * <h2>Fixture</h2>
 * {@code @BeforeAll} creates a single shared test project and four test users with
 * distinct stakeholder permission sets. JWT tokens are obtained via
 * {@code POST /api/auth/login} and reused across all {@code @Test} methods.
 *
 * | Persona        | Stakeholder permissions                        |
 * |----------------|------------------------------------------------|
 * | test-editor    | all [Edit] permissions (Project, Goal, ...)    |
 * | test-deleter   | all [Delete] permissions (no Edit)             |
 * | test-granter   | Goal[Grant] only                               |
 * | test-noaccess  | not added as a stakeholder at all              |
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthorizationIT extends AbstractIntegrationTestCase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // JWT tokens stored once in @BeforeAll and reused in @Test methods
    private String adminToken;
    private String editorToken;
    private String deleterToken;
    private String granterToken;
    private String noAccessToken;

    // Stored so @Test helper methods can create fixture entities via the editor user
    // (admin has no project permissions after the fixture strips the creator grants)
    private String editorUsername;

    // Project and entity IDs needed for delete/edit commands
    private String testProjectName;
    private Long goalId;
    private Integer goalVersion;
    private Long actorId;
    private Integer actorVersion;
    private Long storyId;
    private Integer storyVersion;

    // -------------------------------------------------------------------------
    // Fixture setup
    // -------------------------------------------------------------------------

    @BeforeAll
    void setUpFixture() throws Exception {
        // Baseline initializers normally run via @BeforeEach, but @BeforeAll fires
        // before any @BeforeEach. Ensure admin/project users and seeded permissions
        // are in place before building the test fixture.
        initializeBaselineData();

        User admin = getUserRepository().findUserByUsername("admin");

        // 1. Create a test project owned by admin
        long ts = System.currentTimeMillis();
        testProjectName = "auth-test-" + ts;
        EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
        projectCmd.setEditedBy(admin);
        projectCmd.setName(testProjectName);
        projectCmd.setText("authorization integration test project");
        projectCmd.setOrganizationName("AuthTestOrg-" + ts);
        projectCmd = getCommandHandler().execute(projectCmd);
        Project testProject = projectCmd.getProject();

        // 2. Create test users via EditUser command as admin (must happen before
        //    stripping admin's permissions — EditUserCommand requires SystemAdminUserRole)
        editorUsername = "test-editor-" + ts;
        String deleterUsername = "test-deleter-" + ts;
        String granterUsername = "test-granter-" + ts;
        String noAccessUsername = "test-noaccess-" + ts;
        createUser(editorUsername, "test-editor");
        createUser(deleterUsername, "test-deleter");
        createUser(granterUsername, "test-granter");
        createUser(noAccessUsername, "test-noaccess");

        // 3. Add stakeholders (admin still has all creator permissions — needed to run
        //    EditUserStakeholderCommand before Stakeholder[Edit] is enforced by auth)
        Set<String> allEditPermissions = buildPermissionKeys(StakeholderPermissionType.Edit,
                Project.class, Goal.class, Actor.class, Story.class, UseCase.class,
                Scenario.class, GlossaryTerm.class, Stakeholder.class,
                ReportGenerator.class, Annotation.class);
        addStakeholder(testProject, editorUsername, allEditPermissions);

        Set<String> allDeletePermissions = buildPermissionKeys(StakeholderPermissionType.Delete,
                Goal.class, Actor.class, Story.class, UseCase.class,
                Scenario.class, GlossaryTerm.class, Stakeholder.class,
                ReportGenerator.class, Annotation.class);
        // Note: Project has no Delete permission — intentionally excluded
        addStakeholder(testProject, deleterUsername, allDeletePermissions);

        addStakeholder(testProject, granterUsername,
                Set.of(StakeholderPermissionImpl.generatePermissionKey(
                        Goal.class, StakeholderPermissionType.Grant)));

        // "project" user as a no-permissions stakeholder (edit target for stakeholder tests)
        addStakeholder(testProject, "project", java.util.Set.of());

        // 4. test-noaccess is NOT added as a stakeholder

        // 5. Create fixture entities while admin still has all creator permissions.
        //    Using admin here keeps setup simple; authorization is tested via HTTP below.
        EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
        goalCmd.setEditedBy(admin);
        goalCmd.setGoalContainer(testProject);
        goalCmd.setName("auth-test-goal");
        goalCmd.setText("goal for authorization testing");
        goalCmd = getCommandHandler().execute(goalCmd);
        goalId = goalCmd.getGoal().getId();
        goalVersion = goalCmd.getGoal().getVersion();

        EditActorCommand actorCmd = getProjectCommandFactory().newEditActorCommand();
        actorCmd.setEditedBy(admin);
        actorCmd.setActorContainer(testProject);
        actorCmd.setName("auth-test-actor");
        actorCmd.setText("actor for authorization testing");
        actorCmd = getCommandHandler().execute(actorCmd);
        actorId = actorCmd.getActor().getId();
        actorVersion = actorCmd.getActor().getVersion();

        EditStoryCommand storyCmd = getProjectCommandFactory().newEditStoryCommand();
        storyCmd.setEditedBy(admin);
        storyCmd.setStoryContainer(testProject);
        storyCmd.setName("auth-test-story");
        storyCmd.setText("story for authorization testing");
        storyCmd.setStoryTypeName(StoryType.Success.name());
        storyCmd = getCommandHandler().execute(storyCmd);
        storyId = storyCmd.getStory().getId();
        storyVersion = storyCmd.getStory().getVersion();

        // 6. Strip admin's auto-granted creator permissions. Tests asserting admin cannot
        //    perform project operations depend on admin being a no-permission stakeholder.
        //    This must happen AFTER entity creation (above) and stakeholder setup, since
        //    both require admin to have project permissions to execute commands.
        UserStakeholder adminStakeholder = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(testProject, admin);
        EditUserStakeholderCommand stripCmd = getProjectCommandFactory().newEditUserStakeholderCommand();
        stripCmd.setEditedBy(admin);
        stripCmd.setProjectOrDomain(testProject);
        stripCmd.setUsername("admin");
        stripCmd.setStakeholder(adminStakeholder);
        stripCmd.setStakeholderPermissions(java.util.Set.of());
        getCommandHandler().execute(stripCmd);

        // 8. Obtain JWT tokens via the login endpoint
        adminToken = loginAndGetToken("admin", "admin");
        editorToken = loginAndGetToken(editorUsername, "test-editor");
        deleterToken = loginAndGetToken(deleterUsername, "test-deleter");
        granterToken = loginAndGetToken(granterUsername, "test-granter");
        noAccessToken = loginAndGetToken(noAccessUsername, "test-noaccess");
    }

    // -------------------------------------------------------------------------
    // Unauthenticated (401) — these pass immediately; no AuthorizableCommand needed
    // -------------------------------------------------------------------------

    @Test
    void unauthenticatedEditGoalReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("new-goal-unauth")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteGoalReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteGoalJson(goalId, goalVersion)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedEditStoryReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/commands/EditStory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editStoryJson("new-story-unauth")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteActorReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/commands/DeleteActor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteActorJson(actorId, actorVersion)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // EditGoal — requires Goal[Edit] stakeholder permission
    // SystemAdminUserRole does NOT grant project access; admin needs Goal[Edit] as
    // a stakeholder to edit goals — same rule as any other user.
    // TDD: editor→200, deleter→403, granter→403, noaccess→403, admin(no perms)→403
    // Initially: the 403 cases return 200 (no AuthorizableCommand)
    // -------------------------------------------------------------------------

    @Test
    void adminWithoutGoalEditPermissionCannotEditGoal() throws Exception {
        // Admin is a stakeholder on the project (added at creation) but has no
        // Goal[Edit] permission — project access requires explicit stakeholder grants.
        mockMvc.perform(post("/api/commands/EditGoal")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("admin-goal-attempt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorCanEditGoal() throws Exception {
        mockMvc.perform(post("/api/commands/EditGoal")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("editor-goal-" + System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleterCannotEditGoal() throws Exception {
        mockMvc.perform(post("/api/commands/EditGoal")
                        .header("Authorization", "Bearer " + deleterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("deleter-goal-attempt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void granterCannotEditGoal() throws Exception {
        mockMvc.perform(post("/api/commands/EditGoal")
                        .header("Authorization", "Bearer " + granterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("granter-goal-attempt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAccessCannotEditGoal() throws Exception {
        mockMvc.perform(post("/api/commands/EditGoal")
                        .header("Authorization", "Bearer " + noAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editGoalJson("noaccess-goal-attempt")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // DeleteGoal — requires Goal[Delete]
    // SystemAdminUserRole does NOT grant project access — same as for Edit.
    // TDD: admin(no perms)→403, editor→403, deleter→200, granter→403, noaccess→403
    // Initially: all 403 cases return 200 (no AuthorizableCommand)
    //
    // Note: each delete test needs a fresh goal because delete is destructive.
    // Admin creates these goals in @Test methods directly via CommandHandler.
    // -------------------------------------------------------------------------

    @Test
    void adminWithoutPermissionsCannotDeleteGoal() throws Exception {
        Long id = createGoalForDeleteTest("admin-cannot-delete-goal-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteGoalJson(id, 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleterCanDeleteGoal() throws Exception {
        Long id = createGoalForDeleteTest("deleter-deletes-goal-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .header("Authorization", "Bearer " + deleterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteGoalJson(id, 0)))
                .andExpect(status().isOk());
    }

    @Test
    void editorCannotDeleteGoal() throws Exception {
        Long id = createGoalForDeleteTest("editor-cannot-delete-goal-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteGoalJson(id, 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAccessCannotDeleteGoal() throws Exception {
        Long id = createGoalForDeleteTest("noaccess-cannot-delete-goal-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .header("Authorization", "Bearer " + noAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteGoalJson(id, 0)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // EditStory — requires Story[Edit]
    // -------------------------------------------------------------------------

    @Test
    void editorCanEditStory() throws Exception {
        mockMvc.perform(post("/api/commands/EditStory")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editStoryJson("editor-story-" + System.currentTimeMillis())))
                .andExpect(status().isOk());
    }

    @Test
    void deleterCannotEditStory() throws Exception {
        mockMvc.perform(post("/api/commands/EditStory")
                        .header("Authorization", "Bearer " + deleterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editStoryJson("deleter-story-attempt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAccessCannotEditStory() throws Exception {
        mockMvc.perform(post("/api/commands/EditStory")
                        .header("Authorization", "Bearer " + noAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editStoryJson("noaccess-story-attempt")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // DeleteActor — requires Actor[Delete]
    // -------------------------------------------------------------------------

    @Test
    void deleterCanDeleteActor() throws Exception {
        Long id = createActorForDeleteTest("deleter-deletes-actor-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteActor")
                        .header("Authorization", "Bearer " + deleterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteActorJson(id, 0)))
                .andExpect(status().isOk());
    }

    @Test
    void editorCannotDeleteActor() throws Exception {
        Long id = createActorForDeleteTest("editor-cannot-delete-actor-" + System.currentTimeMillis());
        mockMvc.perform(post("/api/commands/DeleteActor")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deleteActorJson(id, 0)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // EditUserStakeholder — requires Stakeholder[Edit]
    // SystemAdminUserRole does NOT grant project access — stakeholder permissions only.
    // TDD: editor→200, admin(no perms)→403, granter→403, noaccess→403
    // Initially: the 403 cases return 200 (no AuthorizableCommand)
    // -------------------------------------------------------------------------

    @Test
    void editorCanEditUserStakeholder() throws Exception {
        mockMvc.perform(post("/api/commands/EditUserStakeholder")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserStakeholderJson("project")))
                .andExpect(status().isOk());
    }

    @Test
    void adminWithoutPermissionsCannotEditUserStakeholder() throws Exception {
        // Admin has no Stakeholder[Edit] permission on the project — project access
        // is entirely stakeholder-permission-based, not role-based.
        mockMvc.perform(post("/api/commands/EditUserStakeholder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserStakeholderJson("project")))
                .andExpect(status().isForbidden());
    }

    @Test
    void granterCannotEditUserStakeholder() throws Exception {
        // Goal[Grant] does not satisfy Stakeholder[Edit]
        mockMvc.perform(post("/api/commands/EditUserStakeholder")
                        .header("Authorization", "Bearer " + granterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserStakeholderJson("project")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAccessCannotEditUserStakeholder() throws Exception {
        mockMvc.perform(post("/api/commands/EditUserStakeholder")
                        .header("Authorization", "Bearer " + noAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserStakeholderJson("project")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // EditProject — requires Project[Edit]
    // SystemAdminUserRole does NOT grant project access — stakeholder permissions only.
    // TDD: editor→200, admin(no perms)→403, deleter→403, noaccess→403
    // Initially: the 403 cases return 200 (no AuthorizableCommand)
    // -------------------------------------------------------------------------

    @Test
    void adminWithoutPermissionsCannotEditProject() throws Exception {
        // Admin created the project but has no Project[Edit] stakeholder permission —
        // project access requires explicit grants regardless of system role.
        mockMvc.perform(post("/api/commands/EditProject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editProjectJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorCanEditProject() throws Exception {
        mockMvc.perform(post("/api/commands/EditProject")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editProjectJson()))
                .andExpect(status().isOk());
    }

    @Test
    void deleterCannotEditProject() throws Exception {
        // Deleter has no Project[Edit] permission (and there's no Project[Delete])
        mockMvc.perform(post("/api/commands/EditProject")
                        .header("Authorization", "Bearer " + deleterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editProjectJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAccessCannotEditProject() throws Exception {
        mockMvc.perform(post("/api/commands/EditProject")
                        .header("Authorization", "Bearer " + noAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editProjectJson()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // EditUser — requires SystemAdminUserRole (not a stakeholder permission)
    // This is the only project command gated on a system role, not a project permission.
    // TDD: admin→200, editor(project user only)→403
    // Initially: editor returns 200 (no AuthorizableCommand on EditUser)
    // -------------------------------------------------------------------------

    @Test
    void adminCanEditAnotherUser() throws Exception {
        // Admin has SystemAdminUserRole — the only role that permits user management.
        mockMvc.perform(post("/api/commands/EditUser")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserJson("project")))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotEditAnotherUser() throws Exception {
        // Editor has only ProjectUserRole — no SystemAdminUserRole — so they cannot
        // manage users regardless of any project stakeholder permissions.
        mockMvc.perform(post("/api/commands/EditUser")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editUserJson("project")))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Helpers — fixture creation
    // -------------------------------------------------------------------------

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
        cmd.setOrganizationName("AuthTestOrg");
        cmd.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(cmd);
    }

    private void addStakeholder(Project project, String username,
                                Set<String> permissionKeys) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setUsername(username);
        cmd.setStakeholderPermissions(permissionKeys);
        getCommandHandler().execute(cmd);
    }

    private Long createGoalForDeleteTest(String name) throws Exception {
        // Use the editor (who has Goal[Edit]) — admin has no project permissions after setup.
        Project project = getProjectRepository().findProjectByName(testProjectName);
        User editor = getUserRepository().findUserByUsername(editorUsername);
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(editor);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("delete target");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal().getId();
    }

    private Long createActorForDeleteTest(String name) throws Exception {
        // Use the editor (who has Actor[Edit]) — admin has no project permissions after setup.
        Project project = getProjectRepository().findProjectByName(testProjectName);
        User editor = getUserRepository().findUserByUsername(editorUsername);
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(editor);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("delete target");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor().getId();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    @SafeVarargs
    private static Set<String> buildPermissionKeys(StakeholderPermissionType type,
                                                   Class<?>... entityTypes) {
        return java.util.Arrays.stream(entityTypes)
                .map(c -> StakeholderPermissionImpl.generatePermissionKey(c, type))
                .collect(Collectors.toSet());
    }

    // -------------------------------------------------------------------------
    // Helpers — JSON request bodies
    // -------------------------------------------------------------------------

    private String editGoalJson(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "name", name,
                "text", "authorization test goal"));
    }

    private String deleteGoalJson(Long id, Integer version) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "goalId", id,
                "version", version));
    }

    private String editStoryJson(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "name", name,
                "text", "authorization test story",
                "storyTypeName", "Success"));
    }

    private String deleteActorJson(Long id, Integer version) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "actorId", id,
                "version", version));
    }

    private String editUserStakeholderJson(String username) throws Exception {
        // version=0 signals an edit (not create) so tests are idempotent across ordering
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "username", username,
                "permissionKeys", java.util.List.of(),
                "version", 0));
    }

    private String editProjectJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "projectName", testProjectName,
                "name", testProjectName,
                "description", "updated description"));
    }

    private String editUserJson(String username) throws Exception {
        // Edit the named user — sets minimal fields to satisfy validation.
        // version=0 works for an existing user loaded by username.
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "version", 0,
                "name", username,
                "emailAddress", username + "@example.com",
                "phoneNumber", "",
                "organizationName", "AuthTestOrg",
                "userRoleNames", java.util.Set.of("ProjectUserRole")));
    }
}
