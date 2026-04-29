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
package com.rreganjr.requel.project.impl.command;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.CopyActorCommand;
import com.rreganjr.requel.project.command.CopyGoalCommand;
import com.rreganjr.requel.project.command.CopyScenarioCommand;
import com.rreganjr.requel.project.command.CopyScenarioStepCommand;
import com.rreganjr.requel.project.command.CopyStoryCommand;
import com.rreganjr.requel.project.command.CopyUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.user.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the six Copy commands:
 * {@link CopyGoalCommand}, {@link CopyActorCommand}, {@link CopyStoryCommand},
 * {@link CopyUseCaseCommand}, {@link CopyScenarioCommand}, and
 * {@link CopyScenarioStepCommand}.
 *
 * Each copy command auto-generates a unique name by appending " 1", " 2", etc.
 * when the source name is already taken. Tests verify content preservation and
 * that auto-naming produces the expected suffix.
 */
public class CopyCommandTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project createProject(String label) throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(label + "-" + ts);
        cmd.setText("test project for " + label);
        cmd.setOrganizationName("CopyTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Goal createGoal(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("Goal text for " + name + ".");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("Actor text for " + name + ".");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private Story createStory(Project project, String name, String storyTypeName) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("Story text for " + name + ".");
        cmd.setStoryTypeName(storyTypeName);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    private UseCase createUseCase(Project project, String name, String actorName) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("Use case text for " + name + ".");
        cmd.setPrimaryActorName(actorName);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUseCase();
    }

    private Scenario createScenario(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("Scenario text for " + name + ".");
        cmd.setScenarioTypeName(ScenarioType.Primary.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getScenario();
    }

    private Scenario createScenarioWithSteps(Project project, String name, String... stepNames)
            throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        List<EditScenarioStepCommand> stepCommands = new ArrayList<>();
        for (String stepName : stepNames) {
            EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
            stepCmd.setEditedBy(admin);
            stepCmd.setProjectOrDomain(project);
            stepCmd.setName(stepName);
            stepCmd.setText("Text for step: " + stepName);
            stepCmd.setScenarioTypeName(ScenarioType.Primary.name());
            stepCommands.add(stepCmd);
        }
        EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("Scenario text for " + name + ".");
        cmd.setScenarioTypeName(ScenarioType.Primary.name());
        cmd.setStepCommands(stepCommands);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getScenario();
    }

    // -------------------------------------------------------------------------
    // CopyGoalCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyGoalAutoGeneratesUniqueName() throws Exception {
        Project project = createProject("Copy-Goal-auto");
        User admin = getUserRepository().findUserByUsername("admin");
        Goal original = createGoal(project, "Improve communication");

        CopyGoalCommand cmd = getProjectCommandFactory().newCopyGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalGoal(original);
        cmd = getCommandHandler().execute(cmd);

        Goal copy = cmd.getNewGoal();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Improve communication 1", copy.getName(),
                "auto-generated name should append ' 1' when the original name is taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve goal text");
    }

    @Test
    public void copyGoalWithExplicitName() throws Exception {
        Project project = createProject("Copy-Goal-explicit");
        User admin = getUserRepository().findUserByUsername("admin");
        Goal original = createGoal(project, "Support multi-user");

        CopyGoalCommand cmd = getProjectCommandFactory().newCopyGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalGoal(original);
        cmd.setNewGoalName("Support multi-user (revised)");
        cmd = getCommandHandler().execute(cmd);

        Goal copy = cmd.getNewGoal();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Support multi-user (revised)", copy.getName(),
                "explicit name should be used as-is when it is not already taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve goal text");
    }

    // -------------------------------------------------------------------------
    // CopyActorCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyActorAutoGeneratesUniqueName() throws Exception {
        Project project = createProject("Copy-Actor-auto");
        User admin = getUserRepository().findUserByUsername("admin");
        Actor original = createActor(project, "Administrator");

        CopyActorCommand cmd = getProjectCommandFactory().newCopyActorCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalActor(original);
        cmd = getCommandHandler().execute(cmd);

        Actor copy = cmd.getNewActor();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Administrator 1", copy.getName(),
                "auto-generated name should append ' 1' when the original name is taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve actor text");
    }

    @Test
    public void copyActorWithExplicitName() throws Exception {
        Project project = createProject("Copy-Actor-explicit");
        User admin = getUserRepository().findUserByUsername("admin");
        Actor original = createActor(project, "Support Agent");

        CopyActorCommand cmd = getProjectCommandFactory().newCopyActorCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalActor(original);
        cmd.setNewActorName("Support Agent Copy");
        cmd = getCommandHandler().execute(cmd);

        Actor copy = cmd.getNewActor();
        assertEquals("Support Agent Copy", copy.getName(), "explicit actor name should be used");
        assertEquals(original.getText(), copy.getText(), "copy should preserve actor text");
    }

    // -------------------------------------------------------------------------
    // CopyStoryCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyStoryPreservesStoryType() throws Exception {
        Project project = createProject("Copy-Story");
        User admin = getUserRepository().findUserByUsername("admin");
        Story original = createStory(project, "Positive outcome", StoryType.Success.name());

        CopyStoryCommand cmd = getProjectCommandFactory().newCopyStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalStory(original);
        cmd = getCommandHandler().execute(cmd);

        Story copy = cmd.getNewStory();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Positive outcome 1", copy.getName(),
                "auto-generated name should append ' 1' when the original name is taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve story text");
        assertEquals(original.getStoryType(), copy.getStoryType(),
                "copy should preserve the story type");
    }

    @Test
    public void copyStoryWithExplicitName() throws Exception {
        Project project = createProject("Copy-Story-explicit");
        User admin = getUserRepository().findUserByUsername("admin");
        Story original = createStory(project, "Checkout flow", StoryType.Success.name());

        CopyStoryCommand cmd = getProjectCommandFactory().newCopyStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalStory(original);
        cmd.setNewStoryName("Checkout flow Copy");
        cmd = getCommandHandler().execute(cmd);

        Story copy = cmd.getNewStory();
        assertEquals("Checkout flow Copy", copy.getName(), "explicit story name should be used");
        assertEquals(original.getStoryType(), copy.getStoryType(),
                "copy should preserve the story type");
    }

    // -------------------------------------------------------------------------
    // CopyUseCaseCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyUseCasePreservesContentAndCopiesScenario() throws Exception {
        Project project = createProject("Copy-UseCase");
        User admin = getUserRepository().findUserByUsername("admin");
        // EditUseCaseCommand auto-creates a primary scenario
        UseCase original = createUseCase(project, "Login to the system", "Any User");
        assertNotNull(original.getScenario(),
                "pre-condition: use case must have an auto-created primary scenario");

        CopyUseCaseCommand cmd = getProjectCommandFactory().newCopyUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalUseCase(original);
        cmd = getCommandHandler().execute(cmd);

        UseCase copy = cmd.getNewUseCase();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Login to the system 1", copy.getName(),
                "auto-generated name should append ' 1' when the original name is taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve use case text");
        assertNotNull(copy.getScenario(), "copy should have a primary scenario");
        assertNotEquals(original.getScenario().getId(), copy.getScenario().getId(),
                "copy's scenario should be a distinct entity from the original's scenario");
    }

    @Test
    public void copyUseCaseWithExplicitName() throws Exception {
        Project project = createProject("Copy-UseCase-explicit");
        User admin = getUserRepository().findUserByUsername("admin");
        UseCase original = createUseCase(project, "Reset password", "Any User");

        CopyUseCaseCommand cmd = getProjectCommandFactory().newCopyUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalUseCase(original);
        cmd.setNewUseCaseName("Reset password Copy");
        cmd = getCommandHandler().execute(cmd);

        UseCase copy = cmd.getNewUseCase();
        assertEquals("Reset password Copy", copy.getName(), "explicit use case name should be used");
        assertNotNull(copy.getScenario(), "copied use case should still include its scenario");
    }

    // -------------------------------------------------------------------------
    // CopyScenarioCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyScenarioPreservesTypeAndText() throws Exception {
        Project project = createProject("Copy-Scenario");
        User admin = getUserRepository().findUserByUsername("admin");
        Scenario original = createScenario(project, "Happy path");

        CopyScenarioCommand cmd = getProjectCommandFactory().newCopyScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalScenario(original);
        cmd = getCommandHandler().execute(cmd);

        Scenario copy = cmd.getNewScenario();
        assertNotNull(copy, "copy should have been created");
        assertEquals("Happy path 1", copy.getName(),
                "auto-generated name should append ' 1' when the original name is taken");
        assertEquals(original.getText(), copy.getText(), "copy should preserve scenario text");
        assertEquals(original.getType(), copy.getType(), "copy should preserve scenario type");
    }

    @Test
    public void copyScenarioWithExplicitName() throws Exception {
        Project project = createProject("Copy-Scenario-explicit");
        User admin = getUserRepository().findUserByUsername("admin");
        Scenario original = createScenario(project, "Alternate path");

        CopyScenarioCommand cmd = getProjectCommandFactory().newCopyScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalScenario(original);
        cmd.setNewScenarioName("Alternate path Copy");
        cmd = getCommandHandler().execute(cmd);

        Scenario copy = cmd.getNewScenario();
        assertEquals("Alternate path Copy", copy.getName(), "explicit scenario name should be used");
        assertEquals(original.getType(), copy.getType(), "copy should preserve scenario type");
    }

    @Test
    public void copyScenarioCopiesStepsWithUniqueNames() throws Exception {
        Project project = createProject("Copy-Scenario-Steps");
        User admin = getUserRepository().findUserByUsername("admin");
        Scenario original = createScenarioWithSteps(project, "Main flow",
                "User opens the page", "System displays the form");

        assertEquals(2, original.getSteps().size(), "pre-condition: scenario must have 2 steps");

        CopyScenarioCommand cmd = getProjectCommandFactory().newCopyScenarioCommand();
        cmd.setEditedBy(admin);
        cmd.setOriginalScenario(original);
        cmd = getCommandHandler().execute(cmd);

        Scenario copy = cmd.getNewScenario();
        assertNotNull(copy, "copy should have been created");
        assertEquals(2, copy.getSteps().size(),
                "copy should have the same number of steps as the original");

        // Each step gets a unique name since the originals still exist in the project
        List<String> copiedNames = copy.getSteps().stream()
                .map(Step::getName)
                .collect(Collectors.toList());
        assertTrue(copiedNames.contains("User opens the page 1"),
                "first step copy should auto-generate a unique name");
        assertTrue(copiedNames.contains("System displays the form 1"),
                "second step copy should auto-generate a unique name");

        // Copied steps must be distinct entities from the originals
        List<Long> originalIds = original.getSteps().stream()
                .map(Step::getId)
                .collect(Collectors.toList());
        for (Step copiedStep : copy.getSteps()) {
            assertFalse(originalIds.contains(copiedStep.getId()),
                    "copied step " + copiedStep.getName() + " should be a distinct entity");
        }
    }

    // -------------------------------------------------------------------------
    // CopyScenarioStepCommand
    // -------------------------------------------------------------------------

    @Test
    public void copyScenarioStepPreservesContentAndAutoGeneratesUniqueName() throws Exception {
        Project project = createProject("Copy-Step");
        User admin = getUserRepository().findUserByUsername("admin");

        // Steps can be created standalone via EditScenarioStepCommand without
        // being attached to a scenario at creation time.
        EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
        stepCmd.setEditedBy(admin);
        stepCmd.setProjectOrDomain(project);
        stepCmd.setName("User submits form");
        stepCmd.setText("The user fills in all required fields and clicks Submit.");
        stepCmd.setScenarioTypeName(ScenarioType.Primary.name());
        stepCmd = getCommandHandler().execute(stepCmd);
        Step original = stepCmd.getStep();
        assertNotNull(original, "pre-condition: step should have been created");

        CopyScenarioStepCommand copyCmd = getProjectCommandFactory().newCopyScenarioStepCommand();
        copyCmd.setEditedBy(admin);
        copyCmd.setOriginalScenarioStep(original);
        copyCmd = getCommandHandler().execute(copyCmd);

        Step copy = copyCmd.getNewScenarioStep();
        assertNotNull(copy, "copy should have been created");
        assertNotEquals(original.getId(), copy.getId(),
                "copy should be a distinct entity from the original");
        assertEquals("User submits form 1", copy.getName(),
                "copy should auto-generate a unique step name");
        assertEquals(original.getText(), copy.getText(), "copy should preserve step text");
        assertEquals(original.getType(), copy.getType(), "copy should preserve step type");
    }
}
