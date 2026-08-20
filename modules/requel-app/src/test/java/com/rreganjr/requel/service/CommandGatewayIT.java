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

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.command.Command;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.gateway.GatewayPolicyConfig;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the in-process {@link CommandGateway} (issue #69 Slice 3). Exercises the
 * gateway boundary on top of the real command chain + H2 baseline: allow/deny policy, error-kind
 * mapping, per-stakeholder authorization (run as the {@code SecurityContext} user, exactly as the
 * REST controller does), the non-user-stakeholder delete guard, and that loaded-state edit
 * semantics suffice without wiring a separate optimistic-lock version.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommandGatewayIT extends AbstractIntegrationTestCase {

    @Autowired
    private CommandGateway gateway;

    @Autowired
    private CommandRegistry commandRegistry;

    private String projectName;
    private String editorUsername;
    private String noAccessUsername;
    private Long goalId;
    private Long nonUserStakeholderId;
    private Long userStakeholderId;
    private Long storyId;
    private Long actorId;
    private Long actorId2;
    private Long useCaseId;
    private Long storyId2;

    @BeforeAll
    void setUpFixture() throws Exception {
        initializeBaselineData();
        User admin = getUserRepository().findUserByUsername("admin");

        long ts = System.currentTimeMillis();
        projectName = "gw-test-" + ts;
        EditProjectCommand projectCmd = getProjectCommandFactory().newEditProjectCommand();
        projectCmd.setEditedBy(admin);
        projectCmd.setName(projectName);
        projectCmd.setText("gateway integration test project");
        projectCmd.setOrganizationName("GwTestOrg-" + ts);
        projectCmd = getCommandHandler().execute(projectCmd);
        Project project = projectCmd.getProject();

        // A "power" project user with both Edit and Delete across the entities the gateway exposes,
        // so authorization never masks the behaviours under test (deny/notfound/invalid/guard).
        editorUsername = "gw-editor-" + ts;
        noAccessUsername = "gw-noaccess-" + ts;
        createUser(editorUsername);
        createUser(noAccessUsername);

        Set<String> perms = new HashSet<>();
        perms.addAll(keys(StakeholderPermissionType.Edit, Project.class, Goal.class, Actor.class,
                Story.class, UseCase.class, Scenario.class, GlossaryTerm.class, Stakeholder.class,
                ReportGenerator.class, Annotation.class));
        perms.addAll(keys(StakeholderPermissionType.Delete, Goal.class, Actor.class, Story.class,
                UseCase.class, Scenario.class, GlossaryTerm.class, Stakeholder.class,
                ReportGenerator.class, Annotation.class));
        addUserStakeholder(project, editorUsername, perms);

        // A user stakeholder (the seeded "project" user) — DeleteStakeholder must be refused on it.
        addUserStakeholder(project, "project", Set.of());
        UserStakeholder projectStakeholder = getProjectRepository()
                .findStakeholderByProjectOrDomainAndUser(project,
                        getUserRepository().findUserByUsername("project"));
        userStakeholderId = projectStakeholder.getId();

        // A non-user stakeholder — DeleteStakeholder is permitted on it.
        EditNonUserStakeholderCommand nonUserCmd = getProjectCommandFactory()
                .newEditNonUserStakeholderCommand();
        nonUserCmd.setEditedBy(admin);
        nonUserCmd.setProjectOrDomain(project);
        nonUserCmd.setName("vendor-" + ts);
        nonUserCmd.setText("external non-user stakeholder");
        nonUserCmd = getCommandHandler().execute(nonUserCmd);
        nonUserStakeholderId = ((NonUserStakeholder) nonUserCmd.getStakeholder()).getId();

        // A goal to edit (created by admin who holds creator permissions).
        EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
        goalCmd.setEditedBy(admin);
        goalCmd.setGoalContainer(project);
        goalCmd.setName("gw-goal-" + ts);
        goalCmd.setText("initial");
        goalCmd = getCommandHandler().execute(goalCmd);
        goalId = goalCmd.getGoal().getId();

        // A real story, so story-container resolution tests fail only on the container id.
        EditStoryCommand storyCmd = getProjectCommandFactory().newEditStoryCommand();
        storyCmd.setEditedBy(admin);
        storyCmd.setStoryContainer(project);
        storyCmd.setName("gw-story-" + ts);
        storyCmd.setText("gateway story-container test story");
        storyCmd.setStoryTypeName(StoryType.Success.name());
        storyCmd = getCommandHandler().execute(storyCmd);
        storyId = storyCmd.getStory().getId();

        // A second story, so there is a story id that is NOT also a use case id (ids are
        // per-table auto-increment and the first story/use case collide at 1). issue #189
        EditStoryCommand storyCmd2 = getProjectCommandFactory().newEditStoryCommand();
        storyCmd2.setEditedBy(admin);
        storyCmd2.setStoryContainer(project);
        storyCmd2.setName("gw-story2-" + ts);
        storyCmd2.setText("second gateway story-container test story");
        storyCmd2.setStoryTypeName(StoryType.Success.name());
        storyCmd2 = getCommandHandler().execute(storyCmd2);
        storyId2 = storyCmd2.getStory().getId();

        // An actor and a use case, so actor/goal-container resolution tests can target a use case
        // (and an actor) by id with an explicit containerType (issue #189).
        EditActorCommand actorCmd = getProjectCommandFactory().newEditActorCommand();
        actorCmd.setEditedBy(admin);
        actorCmd.setActorContainer(project);
        actorCmd.setName("gw-actor-" + ts);
        actorCmd.setText("gateway actor-container test actor");
        actorCmd = getCommandHandler().execute(actorCmd);
        actorId = actorCmd.getActor().getId();

        // A second actor that is nobody's primary actor, safe to add as a secondary member.
        EditActorCommand actorCmd2 = getProjectCommandFactory().newEditActorCommand();
        actorCmd2.setEditedBy(admin);
        actorCmd2.setActorContainer(project);
        actorCmd2.setName("gw-actor2-" + ts);
        actorCmd2.setText("gateway secondary-actor test actor");
        actorCmd2 = getCommandHandler().execute(actorCmd2);
        actorId2 = actorCmd2.getActor().getId();

        EditUseCaseCommand useCaseCmd = getProjectCommandFactory().newEditUseCaseCommand();
        useCaseCmd.setEditedBy(admin);
        useCaseCmd.setProjectOrDomain(project);
        useCaseCmd.setName("gw-usecase-" + ts);
        useCaseCmd.setText("gateway use-case-container test use case");
        useCaseCmd.setPrimaryActorName(actorCmd.getActor().getName());
        useCaseCmd = getCommandHandler().execute(useCaseCmd);
        useCaseId = useCaseCmd.getUseCase().getId();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- behaviour -----------------------------------------------------------------------------

    @Test
    void allowedCommandExecutesAsAuthorizedUser() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("EditGoal",
                Map.of("projectName", projectName, "name", "gw-created-" + System.currentTimeMillis(),
                        "text", "made via gateway")));
        assertEquals("EditGoal", result.commandType());
        assertNotNull(result.result(), "EditGoal should return a goal DTO");
    }

    @Test
    void unauthorizedUserIsRejected() {
        authenticate(noAccessUsername);
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("EditGoal",
                        Map.of("projectName", projectName, "name", "blocked", "text", "x"))));
        assertEquals(GatewayException.Kind.UNAUTHORIZED, ex.getKind());
    }

    @Test
    void denylistedCommandIsNotAllowed() {
        authenticate(editorUsername);
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("EditUser", Map.of("username", "project", "version", 0))));
        assertEquals(GatewayException.Kind.NOT_ALLOWED, ex.getKind());
    }

    @Test
    void unknownCommandIsNotFound() {
        authenticate(editorUsername);
        GatewayException ex = assertThrows(GatewayException.class,
                () -> gateway.execute(new GatewayRequest("NoSuchCommand", Map.of())));
        assertEquals(GatewayException.Kind.NOT_FOUND, ex.getKind());
    }

    @Test
    void malformedInputIsInvalid() {
        authenticate(editorUsername);
        // goalId must be a Long; a non-numeric string cannot be bound to EditGoalInput.
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("EditGoal",
                        Map.of("projectName", projectName, "goalId", "not-a-number", "name", "x"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void addStoryToStakeholderContainerIsInvalid() {
        authenticate(editorUsername);
        // A stakeholder is not a StoryContainer (issue #187). Passing its id as
        // storyContainerId is a caller error, so the gateway must map it to INVALID_INPUT
        // rather than a ClassCastException surfaced as EXECUTION_ERROR.
        Project project = getProjectRepository().findProjectByName(projectName);
        assertNotEquals(project.getId(), userStakeholderId,
                "fixture precondition: stakeholder id must not collide with the project id");
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddStoryToStoryContainer",
                        Map.of("projectName", projectName, "storyContainerId", userStakeholderId,
                                "storyId", storyId, "containerType", "Stakeholder"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void removeStoryFromStakeholderContainerIsInvalid() {
        authenticate(editorUsername);
        Project project = getProjectRepository().findProjectByName(projectName);
        assertNotEquals(project.getId(), userStakeholderId,
                "fixture precondition: stakeholder id must not collide with the project id");
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("RemoveStoryFromStoryContainer",
                        Map.of("projectName", projectName, "storyContainerId", userStakeholderId,
                                "storyId", storyId, "containerType", "Stakeholder"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void storyContainerWithUnknownIdIsInvalid() {
        authenticate(editorUsername);
        // An id matching neither the project nor any use case is not a story container.
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddStoryToStoryContainer",
                        Map.of("projectName", projectName, "storyContainerId", 999_999_999L,
                                "storyId", storyId, "containerType", "UseCase"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void storyContainerMissingContainerTypeIsInvalid() {
        authenticate(editorUsername);
        // containerType is required (issue #189); omitting it is a caller error even when the id
        // names a real story container.
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddStoryToStoryContainer",
                        Map.of("projectName", projectName, "storyContainerId", useCaseId,
                                "storyId", storyId))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void actorContainerLookupIsScopedToNamedType() {
        authenticate(editorUsername);
        // Issue #189 core regression: the type-scoped lookup must not fall through to other
        // collections the way the pre-fix scan-all lookup did. storyId2 exists only as a story
        // (there is no use case with that id), so requesting it as a "UseCase" actor container is
        // rejected. The old lookup would have matched the story (a valid ActorContainer) and
        // silently attached the actor to the wrong parent.
        assertNotEquals(useCaseId, storyId2,
                "fixture precondition: the second story id must not also be a use case id");
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddActorToActorContainer",
                        Map.of("projectName", projectName, "actorContainerId", storyId2,
                                "actorId", actorId, "containerType", "UseCase"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
    }

    @Test
    void goalContainerResolvesUseCaseByExplicitType() throws Exception {
        authenticate(editorUsername);
        // Positive path for the type-scoped goal lookup (issue #189): resolving a use case goal
        // container by its named type executes without a resolution error.
        GatewayResult result = gateway.execute(new GatewayRequest("AddGoalToGoalContainer",
                Map.of("projectName", projectName, "goalContainerId", useCaseId,
                        "goalId", goalId, "containerType", "UseCase")));
        assertEquals("AddGoalToGoalContainer", result.commandType());
    }

    // ---- issue #189: type-scoped container resolution ------------------------------------------
    // These exercise each branch of findStoryContainerById / findActorContainerById /
    // findGoalContainerById through the gateway. The backend coverage is measured from this Java
    // run (JaCoCo), not the Playwright e2e suite, so the happy paths must be pinned here too.

    @Test
    void storyContainerResolvesUseCaseByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddStoryToStoryContainer",
                Map.of("projectName", projectName, "storyContainerId", useCaseId,
                        "storyId", storyId, "containerType", "UseCase")));
        assertEquals("AddStoryToStoryContainer", result.commandType());
    }

    @Test
    void actorContainerResolvesUseCaseByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddActorToActorContainer",
                Map.of("projectName", projectName, "actorContainerId", useCaseId,
                        "actorId", actorId2, "containerType", "UseCase")));
        assertEquals("AddActorToActorContainer", result.commandType());
    }

    @Test
    void actorContainerResolvesStoryByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddActorToActorContainer",
                Map.of("projectName", projectName, "actorContainerId", storyId,
                        "actorId", actorId2, "containerType", "Story")));
        assertEquals("AddActorToActorContainer", result.commandType());
    }

    @Test
    void goalContainerResolvesStoryByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddGoalToGoalContainer",
                Map.of("projectName", projectName, "goalContainerId", storyId,
                        "goalId", goalId, "containerType", "Story")));
        assertEquals("AddGoalToGoalContainer", result.commandType());
    }

    @Test
    void goalContainerResolvesActorByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddGoalToGoalContainer",
                Map.of("projectName", projectName, "goalContainerId", actorId,
                        "goalId", goalId, "containerType", "Actor")));
        assertEquals("AddGoalToGoalContainer", result.commandType());
    }

    @Test
    void goalContainerResolvesStakeholderByExplicitType() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("AddGoalToGoalContainer",
                Map.of("projectName", projectName, "goalContainerId", userStakeholderId,
                        "goalId", goalId, "containerType", "Stakeholder")));
        assertEquals("AddGoalToGoalContainer", result.commandType());
    }

    @Test
    void storyContainerRejectsUnknownProjectId() {
        assertContainerRequestInvalid("AddStoryToStoryContainer",
                Map.of("projectName", projectName, "storyContainerId", 999_999_999L,
                        "storyId", storyId, "containerType", "Project"));
    }

    @Test
    void actorContainerRejectsUnknownProjectId() {
        assertContainerRequestInvalid("AddActorToActorContainer",
                Map.of("projectName", projectName, "actorContainerId", 999_999_999L,
                        "actorId", actorId, "containerType", "Project"));
    }

    @Test
    void actorContainerRejectsUnknownStoryId() {
        assertContainerRequestInvalid("AddActorToActorContainer",
                Map.of("projectName", projectName, "actorContainerId", 999_999_999L,
                        "actorId", actorId, "containerType", "Story"));
    }

    @Test
    void actorContainerRejectsUnknownType() {
        assertContainerRequestInvalid("AddActorToActorContainer",
                Map.of("projectName", projectName, "actorContainerId", useCaseId,
                        "actorId", actorId, "containerType", "Bogus"));
    }

    @Test
    void goalContainerRejectsUnknownIdForEachType() {
        // Not-found within each named type -> INVALID_INPUT (covers every branch throw), plus the
        // terminal unknown-type throw.
        for (String type : new String[] { "Project", "UseCase", "Story", "Actor", "Stakeholder" }) {
            assertContainerRequestInvalid("AddGoalToGoalContainer",
                    Map.of("projectName", projectName, "goalContainerId", 999_999_999L,
                            "goalId", goalId, "containerType", type));
        }
        assertContainerRequestInvalid("AddGoalToGoalContainer",
                Map.of("projectName", projectName, "goalContainerId", useCaseId,
                        "goalId", goalId, "containerType", "Bogus"));
    }

    private void assertContainerRequestInvalid(String commandType, Map<String, Object> input) {
        authenticate(editorUsername);
        GatewayException ex = assertThrows(GatewayException.class,
                () -> gateway.execute(new GatewayRequest(commandType, input)));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind(),
                commandType + " " + input.get("containerType") + " should map to INVALID_INPUT");
    }

    @Test
    void deleteUserStakeholderIsNotAllowed() {
        authenticate(editorUsername);
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("DeleteStakeholder",
                        Map.of("projectName", projectName, "stakeholderId", userStakeholderId))));
        assertEquals(GatewayException.Kind.NOT_ALLOWED, ex.getKind());
    }

    @Test
    void deleteNonUserStakeholderIsPermittedAndExecutes() throws Exception {
        authenticate(editorUsername);
        GatewayResult result = gateway.execute(new GatewayRequest("DeleteStakeholder",
                Map.of("projectName", projectName, "stakeholderId", nonUserStakeholderId)));
        assertEquals("DeleteStakeholder", result.commandType());
    }

    @Test
    void sequentialEditsSucceedWithoutVersion() throws Exception {
        authenticate(editorUsername);
        gateway.execute(new GatewayRequest("EditGoal",
                Map.of("projectName", projectName, "goalId", goalId, "name", "gw-goal-edit",
                        "text", "v1")));
        gateway.execute(new GatewayRequest("EditGoal",
                Map.of("projectName", projectName, "goalId", goalId, "name", "gw-goal-edit",
                        "text", "v2")));
        Project project = getProjectRepository().findProjectByName(projectName);
        Goal goal = project.getGoals().stream().filter(g -> g.getId().equals(goalId)).findFirst()
                .orElseThrow();
        assertEquals("v2", goal.getText(), "loaded-state edit should apply the latest write");
    }

    // ---- policy surface ------------------------------------------------------------------------

    @Test
    void everyAllowlistedCommandIsRegisteredAndAuthorizable() {
        for (String type : GatewayPolicyConfig.ALLOWED) {
            assertTrue(commandRegistry.isRegistered(type), type + " must be registered");
            CommandRegistration<?> reg = commandRegistry.lookup(type);
            assertNotNull(reg.factoryMethod(), type + " must have a factory method");
            Command instance = reg.factoryMethod().get();
            assertTrue(instance instanceof AuthorizableCommand,
                    type + " must implement AuthorizableCommand so the gateway never exposes an "
                            + "unchecked write");
        }
    }

    @Test
    void allowAndDenyListsAreDisjointAndIdentitySafe() {
        for (String denied : GatewayPolicyConfig.DENIED) {
            assertFalse(GatewayPolicyConfig.ALLOWED.contains(denied),
                    denied + " is on both allow and deny lists");
        }
        assertTrue(GatewayPolicyConfig.DENIED.containsAll(
                Set.of("Login", "EditUser", "EditUserStakeholder")),
                "identity/user commands must be denied");
    }

    // ---- helpers -------------------------------------------------------------------------------

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x", List.of()));
    }

    private void createUser(String username) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
        cmd.setEditedBy(admin);
        cmd.setUsername(username);
        cmd.setPassword("pw");
        cmd.setRepassword("pw");
        cmd.setName(username);
        cmd.setEmailAddress(username + "@example.com");
        cmd.setPhoneNumber("");
        cmd.setOrganizationName("GwTestOrg");
        cmd.addUserRoleName("ProjectUserRole");
        getCommandHandler().execute(cmd);
    }

    private void addUserStakeholder(Project project, String username, Set<String> permissionKeys)
            throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setUsername(username);
        cmd.setStakeholderPermissions(permissionKeys);
        getCommandHandler().execute(cmd);
    }

    @SafeVarargs
    private static Set<String> keys(StakeholderPermissionType type, Class<?>... entityTypes) {
        return Arrays.stream(entityTypes)
                .map(c -> StakeholderPermissionImpl.generatePermissionKey(c, type))
                .collect(Collectors.toSet());
    }
}
