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

import com.rreganjr.requel.service.api.dto.DictionaryWordDto;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.command.Command;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.platform.exception.EntityLockException;
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
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.service.api.dto.ScenarioDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.UseCaseDto;
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
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.gateway.QueryGateway;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

        // Ids are per-table auto-increment, so whether storyId2 collides with useCaseId depends
        // on how many stories and use cases earlier test classes created in the shared context
        // (it did, at 5, once DeleteCascadeIT joined the suite). The #189 regression test needs
        // a story id that is NOT a use case id, so keep adding stories until it has one.
        for (int extra = 0; storyId2.equals(useCaseId); extra++) {
            EditStoryCommand storyCmdN = getProjectCommandFactory().newEditStoryCommand();
            storyCmdN.setEditedBy(admin);
            storyCmdN.setStoryContainer(project);
            storyCmdN.setName("gw-story2-" + ts + "-" + extra);
            storyCmdN.setText("story whose id is not also a use case id");
            storyCmdN.setStoryTypeName(StoryType.Success.name());
            storyCmdN = getCommandHandler().execute(storyCmdN);
            storyId2 = storyCmdN.getStory().getId();
        }
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

    /** Issue #319: the project dictionary pair is on the gateway, gated on Project[Edit]. */
    @Test
    void projectDictionaryWordsCanBeAddedAndRemovedThroughTheGateway() throws Exception {
        authenticate(editorUsername);
        String lemma = "gwdictword" + System.nanoTime();
        GatewayResult added = gateway.execute(new GatewayRequest("AddProjectDictionaryWord",
                Map.of("projectName", projectName, "lemma", lemma)));
        DictionaryWordDto word = (DictionaryWordDto) added.result();
        assertEquals(lemma, word.lemma());

        gateway.execute(new GatewayRequest("DeleteProjectDictionaryWord",
                Map.of("projectName", projectName, "wordId", word.id())));

        Project project = getProjectRepository().findProjectByName(projectName);
        assertFalse(getDictionaryRepository().isKnownWord(project.getId(), lemma),
                "the removed word should be unknown in the project");
    }

    /** Issue #319: the installation dictionary is admin-only and never on the gateway. */
    @Test
    void installationDictionaryCommandsAreNotAllowed() {
        authenticate("admin");
        GatewayException add = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddInstallDictionaryWord", Map.of("lemma", "gwinstall"))));
        assertEquals(GatewayException.Kind.NOT_ALLOWED, add.getKind());
        GatewayException delete = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("DeleteInstallDictionaryWord", Map.of("wordId", 1L))));
        assertEquals(GatewayException.Kind.NOT_ALLOWED, delete.getKind());
    }

    // ---- issue #296: version on deletes, and the annotation read's project check ----------------

    /**
     * The seven entity deletes took a version and dropped it. Now a stale one is refused through
     * the gateway, the entity is kept, and the current version deletes it.
     */
    @Test
    void aDeleteWithAStaleVersionIsRefusedAndTheCurrentVersionDeletes() throws Exception {
        authenticate(editorUsername);
        GoalDto goal = (GoalDto) gateway.execute(new GatewayRequest("EditGoal",
                Map.of("projectName", projectName, "name", "gw-296-" + System.nanoTime(),
                        "text", "to delete"))).result();

        GatewayException stale = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("DeleteGoal", Map.of("projectName", projectName,
                        "goalId", goal.id(), "version", goal.version() + 5))));
        assertTrue(causedByStaleEntity(stale),
                "a stale version should be refused as out of date: " + stale.getMessage());
        assertTrue(goalExists(goal.id()), "a refused delete must leave the goal in place");

        gateway.execute(new GatewayRequest("DeleteGoal", Map.of("projectName", projectName,
                "goalId", goal.id(), "version", goal.version())));
        assertFalse(goalExists(goal.id()), "the current version deletes the goal");
    }

    /** A null version still skips the check, as it does for the edits. */
    @Test
    void aDeleteWithoutAVersionIsNotChecked() throws Exception {
        authenticate(editorUsername);
        GoalDto goal = (GoalDto) gateway.execute(new GatewayRequest("EditGoal",
                Map.of("projectName", projectName, "name", "gw-296-nov-" + System.nanoTime(),
                        "text", "to delete"))).result();

        gateway.execute(new GatewayRequest("DeleteGoal",
                Map.of("projectName", projectName, "goalId", goal.id())));
        assertFalse(goalExists(goal.id()));
    }

    private static boolean causedByStaleEntity(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof EntityLockException) {
                return true;
            }
        }
        return false;
    }

    private boolean goalExists(Long id) throws Exception {
        return getProjectRepository().findProjectByName(projectName).getGoals().stream()
                .anyMatch(g -> g.getId().equals(id));
    }

    /**
     * getAnnotations loaded any entity by type and id for any signed-in user. A user who is not a
     * stakeholder on the project is now refused; a stakeholder still reads.
     */
    @Test
    void annotationsAreReadOnlyByThoseWhoCanReadTheProject() {
        authenticate(noAccessUsername);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> queryGateway.getAnnotations(projectName, "Goal", goalId));
        assertEquals(403, refused.getStatusCode().value());

        // The read walks the entity's lazy annotations, which a web request's open session
        // covers; here a transaction does (as in IssueSeverityIT).
        authenticate(editorUsername);
        assertNotNull(new TransactionTemplate(transactionManager).execute(
                status -> queryGateway.getAnnotations(projectName, "Goal", goalId)));
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

    @Test
    void unknownChildIdIsInvalidInput() {
        authenticate(editorUsername);
        // Valid container, but the child goal id does not exist: findGoalById throws
        // IllegalArgumentException("Goal not found") from the input applicator while the command is
        // built. Before #188 that fell through to EXECUTION_ERROR; it must be INVALID_INPUT so MCP
        // maps it to INVALID_PARAMS (not INTERNAL_ERROR) and the gateway HTTP wrapper returns 400,
        // matching the HTTP controller. This is the child-lookup gap left after #189 scoped the
        // container lookups to EntityValidationException.
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("AddGoalToGoalContainer",
                        Map.of("projectName", projectName, "goalContainerId", useCaseId,
                                "goalId", 999_999_999L, "containerType", "UseCase"))));
        assertEquals(GatewayException.Kind.INVALID_INPUT, ex.getKind());
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

    // ---- DeleteProject (issue #242) -------------------------------------------------------------

    /**
     * Issue #242 AC: an allowlisted {@code DeleteProject} dispatched through the gateway by a
     * stakeholder holding {@code Project[Delete]} actually deletes the project.
     * <p>
     * Deliberately operates on its own throwaway project rather than the class fixture's
     * {@code projectName}, which every other test in this class shares — test order must never let
     * a delete pull the fixture out from under them.
     */
    @Test
    void deleteProjectExecutesForAStakeholderWithProjectDelete() throws Exception {
        long ts = System.nanoTime();
        String targetName = "gw-del-ok-" + ts;
        String username = "gw-del-ok-user-" + ts;
        Project target = createProject(targetName);
        createUser(username);
        addUserStakeholder(target, username,
                keys(StakeholderPermissionType.Delete, Project.class));

        authenticate(username);
        GatewayResult result = gateway.execute(
                new GatewayRequest("DeleteProject", Map.of("projectName", targetName)));

        assertEquals("DeleteProject", result.commandType());
        assertThrows(NoSuchProjectException.class,
                () -> getProjectRepository().findProjectByName(targetName),
                "the project should be gone after a gateway DeleteProject");
    }

    /**
     * Issue #242 AC: a caller without {@code Project[Delete]} is rejected at the <em>command</em>
     * layer, not merely hidden from the tool list. The policy allows the command type — this
     * stakeholder even holds {@code Project[Edit]} — so an UNAUTHORIZED outcome here can only have
     * come from {@code AuthorizingCommandHandler} checking the per-stakeholder permission, and the
     * project must survive.
     */
    @Test
    void deleteProjectIsRejectedForAStakeholderWithoutProjectDelete() throws Exception {
        long ts = System.nanoTime();
        String targetName = "gw-del-denied-" + ts;
        String username = "gw-del-denied-user-" + ts;
        Project target = createProject(targetName);
        createUser(username);
        addUserStakeholder(target, username, keys(StakeholderPermissionType.Edit, Project.class));

        authenticate(username);
        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("DeleteProject", Map.of("projectName", targetName))));

        assertEquals(GatewayException.Kind.UNAUTHORIZED, ex.getKind());
        assertNotNull(getProjectRepository().findProjectByName(targetName),
                "a rejected DeleteProject must leave the project intact");
    }

    /**
     * Issue #252: {@code ConvertStepToScenario} is the one step command kept on the gateway, and it
     * now carries a real input DTO instead of an empty schema. This exercises the applicator end to
     * end, which is the part that could not be proved by inspection: the command is an
     * {@code EditScenarioCommand} underneath and builds its new scenario from name, text and type,
     * so the applicator has to carry those over from the step being converted or the conversion
     * produces an unnamed scenario.
     *
     * <p>The fixture is built through the gateway on purpose — {@code EditScenario} with a steps
     * array is the documented way to create a step, and it is what the removed
     * {@code EditScenarioStep} tool is being replaced by.
     */
    @Test
    void convertStepToScenarioBindsItsInputAndCarriesTheStepsNameOver() throws Exception {
        authenticate(editorUsername);
        String stepName = "gw-step-" + System.currentTimeMillis();

        GatewayResult created = gateway.execute(new GatewayRequest("EditScenario",
                Map.of("projectName", projectName,
                        "name", "gw-scenario-" + System.currentTimeMillis(),
                        "text", "scenario holding one plain step",
                        "scenarioTypeName", ScenarioType.Primary.name(),
                        "steps", List.of(Map.of("name", stepName, "text", "a plain step",
                                "isScenario", false)))));
        ScenarioDto scenario = (ScenarioDto) created.result();
        assertNotNull(scenario, "EditScenario should return a scenario DTO");
        assertEquals(1, scenario.steps().size(), "the scenario should hold the one step sent");
        Long stepId = scenario.steps().get(0).id();

        GatewayResult converted = gateway.execute(new GatewayRequest("ConvertStepToScenario",
                Map.of("projectName", projectName, "stepId", stepId)));

        assertEquals("ConvertStepToScenario", converted.commandType());
        ScenarioDto promoted = (ScenarioDto) converted.result();
        assertNotNull(promoted, "conversion should return the new scenario");
        assertEquals(stepName, promoted.name(),
                "the promoted scenario keeps the step's name, as the tool description promises");
        assertNotEquals(stepId, promoted.id(),
                "conversion creates a distinct scenario entity, not a mutated step");
    }

    /**
     * Issue #254 on the surface it was actually reported from. Sending a steps entry whose name is
     * already taken used to fail with "The name conflicts with an existing ProjectOrDomainEntity",
     * naming neither the step nor the entity it hit, and appearing to blame the scenario — which
     * cost two wrong fixes during the roundtable build before the cause was isolated.
     */
    @Test
    void aStepsEntryWithATakenNameIsRefusedWithAnActionableMessage() throws Exception {
        authenticate(editorUsername);
        String stepName = "gw-shared-step-" + System.currentTimeMillis();

        GatewayResult created = gateway.execute(new GatewayRequest("EditScenario",
                Map.of("projectName", projectName,
                        "name", "gw-first-scenario-" + System.currentTimeMillis(),
                        "text", "holds the step whose name gets reused",
                        "scenarioTypeName", ScenarioType.Primary.name(),
                        "steps", List.of(Map.of("name", stepName, "text", "the original step",
                                "isScenario", false)))));
        Long stepId = ((ScenarioDto) created.result()).steps().get(0).id();

        GatewayException ex = assertThrows(GatewayException.class, () -> gateway.execute(
                new GatewayRequest("EditScenario",
                        Map.of("projectName", projectName,
                                "name", "gw-second-scenario-" + System.currentTimeMillis(),
                                "text", "tries to create a step by the same name",
                                "scenarioTypeName", ScenarioType.Primary.name(),
                                "steps", List.of(Map.of("name", stepName,
                                        "text", "approximate prose from an ingest client",
                                        "isScenario", false))))));

        StringBuilder chain = new StringBuilder();
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                chain.append(t.getMessage()).append(" | ");
            }
        }
        String message = chain.toString();
        assertTrue(message.contains(String.valueOf(stepId)),
                "the refusal must name the existing step's id so the caller can link it: " + message);
        assertTrue(message.contains("stepId"),
                "the refusal must say which field to send it in: " + message);
        assertFalse(message.contains("ProjectOrDomainEntity"),
                "naming the registration key instead of the entity is the bug: " + message);
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
            // Issue #252: CommandRegistry's two-argument register(type, factory) overload is a
            // "placeholder for future DTO wiring" and records Void.class, which the catalog turns
            // into an empty MCP schema — a tool that advertises no way to call it. Four allowlisted
            // commands were in that state and nothing failed. Either give the command a real input
            // DTO or take it off the allowlist.
            assertNotEquals(Void.class, reg.inputClass(),
                    type + " is allowlisted but registered with no input DTO, so it advertises an "
                            + "empty schema and cannot be called");
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
        // #256: the stakeholder-repair command creates stakeholder rows and grants
        // permissions, which is the same category as EditUserStakeholder above.
        assertTrue(GatewayPolicyConfig.DENIED.contains("RepairProjectStakeholders"),
                "administrative stakeholder repair must not be exposed on the gateway");
        // #319: the installation dictionary changes every project, so it stays off the gateway.
        assertTrue(GatewayPolicyConfig.DENIED.containsAll(
                Set.of("AddInstallDictionaryWord", "DeleteInstallDictionaryWord")),
                "installation dictionary commands must not be exposed on the gateway");
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

    /** A throwaway project owned by admin, for tests that destroy what they operate on. */
    private Project createProject(String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(name);
        cmd.setText("gateway DeleteProject test project");
        cmd.setOrganizationName("GwDelOrg-" + name);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    @SafeVarargs
    private static Set<String> keys(StakeholderPermissionType type, Class<?>... entityTypes) {
        return Arrays.stream(entityTypes)
                .map(c -> StakeholderPermissionImpl.generatePermissionKey(c, type))
                .collect(Collectors.toSet());
    }

    // -------------------------------------------------------------------------
    // Steps through the API (issue #325)
    // -------------------------------------------------------------------------

    /** EditScenario without a steps key leaves the steps alone; it used to delete them all. */
    @Test
    void editScenarioWithoutStepsKeepsTheSteps() throws Exception {
        authenticate(editorUsername);
        long ts = System.currentTimeMillis();
        ScenarioDto created = (ScenarioDto) gateway.execute(new GatewayRequest("EditScenario",
                Map.of("projectName", projectName,
                        "name", "gw-keep-steps-" + ts,
                        "scenarioTypeName", ScenarioType.Primary.name(),
                        "steps", List.of(Map.of("name", "gw-kept-step-" + ts, "text", "a step",
                                "isScenario", false))))).result();

        ScenarioDto updated = (ScenarioDto) gateway.execute(new GatewayRequest("EditScenario",
                Map.of("projectName", projectName,
                        "scenarioId", created.id(),
                        "version", created.version(),
                        "name", created.name(),
                        "text", "only the text changes"))).result();

        assertEquals("only the text changes", updated.text());
        assertEquals(1, updated.steps().size(), "an absent steps key should keep the steps");
    }

    /**
     * A use-case save from the editor sends no steps. It used to reach the nested EditScenario
     * as an empty list and wipe the primary scenario's steps.
     */
    @Test
    void editUseCaseKeepsItsPrimaryScenarioSteps() throws Exception {
        // Its own project: a use case created in the shared fixture project can take the id
        // storyId2 was chosen to avoid, and actorContainerLookupIsScopedToNamedType then finds a
        // "UseCase" with that id (CLAUDE.md: ids from different tables collide).
        long ts = System.currentTimeMillis();
        String ucProjectName = "gw-uc-steps-" + ts;
        Project ucProject = createProject(ucProjectName);
        Set<String> perms = new HashSet<>(keys(StakeholderPermissionType.Edit, Project.class,
                Actor.class, UseCase.class, Scenario.class));
        addUserStakeholder(ucProject, editorUsername, perms);
        authenticate(editorUsername);
        String actorName = "gw-uc-actor-" + ts;
        UseCaseDto useCase = (UseCaseDto) gateway.execute(new GatewayRequest("EditUseCase",
                Map.of("projectName", ucProjectName,
                        "name", "gw-uc-" + ts,
                        "text", "a use case",
                        "primaryActorName", actorName))).result();
        ScenarioDto scenario = (ScenarioDto) gateway.execute(new GatewayRequest("EditScenario",
                Map.of("projectName", ucProjectName,
                        "scenarioId", useCase.scenarioId(),
                        "name", useCase.scenarioName(),
                        "steps", List.of(Map.of("name", "gw-uc-step-" + ts, "text", "a step",
                                "isScenario", false))))).result();
        assertEquals(1, scenario.steps().size(), "fixture: the primary scenario has one step");

        gateway.execute(new GatewayRequest("EditUseCase",
                Map.of("projectName", ucProjectName,
                        "useCaseId", useCase.id(),
                        "name", useCase.name(),
                        "text", "the description changes",
                        "primaryActorName", actorName)));

        Scenario reloaded = getProjectRepository().get(
                getProjectRepository().findScenarioByProjectOrDomainAndName(
                        getProjectRepository().findProjectByName(ucProjectName), scenario.name()));
        assertEquals(1, reloaded.getSteps().size(),
                "saving the use case must keep its primary scenario's steps");
    }
}
