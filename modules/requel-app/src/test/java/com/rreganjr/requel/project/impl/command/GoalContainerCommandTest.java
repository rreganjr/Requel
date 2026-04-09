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
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for goal-container relationship commands:
 * {@link AddGoalToGoalContainerCommand} and {@link RemoveGoalFromGoalContainerCommand}.
 *
 * Goals are scoped to a project via {@code EditGoalCommand.setGoalContainer(project)}.
 * The add/remove container commands link that goal into secondary containers —
 * use cases and stories — which track the association in separate join tables.
 *
 * Project is intentionally excluded: project-level goal membership is managed
 * by {@code EditGoalCommand} (the goal's {@code projectOrDomain} FK), not by this
 * command pair.
 */
public class GoalContainerCommandTest extends AbstractIntegrationTestCase {

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
        cmd.setOrganizationName("GoalContainerTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Goal createGoal(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("A goal for container relationship tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("An actor for goal container tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private UseCase createUseCase(Project project, Actor primaryActor, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("A use case for goal container tests.");
        cmd.setPrimaryActorName(primaryActor.getName());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getUseCase();
    }

    private Story createStory(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("A story for goal container tests.");
        cmd.setStoryTypeName(StoryType.Success.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    // -------------------------------------------------------------------------
    // UseCase as container
    // -------------------------------------------------------------------------

    @Test
    public void addGoalToUseCase() throws Exception {
        Project project = createProject("GoalContainer-UC-add");
        Goal goal = createGoal(project, "Improve login security");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Authenticate user");

        assertTrue(useCase.getGoals().isEmpty(),
                "use case should have no goals before the command");

        AddGoalToGoalContainerCommand cmd =
                getProjectCommandFactory().newAddGoalToGoalContainerCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setGoal(goal);
        cmd.setGoalContainer(useCase);
        cmd = getCommandHandler().execute(cmd);

        UseCase updated = (UseCase) cmd.getGoalContainer();
        assertTrue(updated.getGoals().stream()
                        .anyMatch(g -> g.getName().equals("Improve login security")),
                "goal should appear in use case goals after add");
    }

    @Test
    public void removeGoalFromUseCase() throws Exception {
        Project project = createProject("GoalContainer-UC-remove");
        Goal goal = createGoal(project, "Support password reset");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Reset password");

        // First add the goal
        User admin = getUserRepository().findUserByUsername("admin");
        AddGoalToGoalContainerCommand addCmd =
                getProjectCommandFactory().newAddGoalToGoalContainerCommand();
        addCmd.setEditedBy(admin);
        addCmd.setGoal(goal);
        addCmd.setGoalContainer(useCase);
        addCmd = getCommandHandler().execute(addCmd);
        UseCase withGoal = (UseCase) addCmd.getGoalContainer();
        assertFalse(withGoal.getGoals().isEmpty(), "goal should be present after add");

        // Now remove it
        RemoveGoalFromGoalContainerCommand removeCmd =
                getProjectCommandFactory().newRemoveGoalFromGoalContainerCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setGoal(goal);
        removeCmd.setGoalContainer(withGoal);
        removeCmd = getCommandHandler().execute(removeCmd);

        UseCase withoutGoal = (UseCase) removeCmd.getGoalContainer();
        assertTrue(withoutGoal.getGoals().isEmpty(),
                "goal should be absent from use case goals after remove");
    }

    // -------------------------------------------------------------------------
    // Story as container
    // -------------------------------------------------------------------------

    @Test
    public void addGoalToStory() throws Exception {
        Project project = createProject("GoalContainer-Story-add");
        Goal goal = createGoal(project, "Allow remote access");
        Story story = createStory(project, "User logs in remotely");

        assertTrue(story.getGoals().isEmpty(),
                "story should have no goals before the command");

        AddGoalToGoalContainerCommand cmd =
                getProjectCommandFactory().newAddGoalToGoalContainerCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setGoal(goal);
        cmd.setGoalContainer(story);
        cmd = getCommandHandler().execute(cmd);

        Story updated = (Story) cmd.getGoalContainer();
        assertTrue(updated.getGoals().stream()
                        .anyMatch(g -> g.getName().equals("Allow remote access")),
                "goal should appear in story goals after add");
    }

    @Test
    public void removeGoalFromStory() throws Exception {
        Project project = createProject("GoalContainer-Story-remove");
        Goal goal = createGoal(project, "Track user activity");
        Story story = createStory(project, "Admin reviews audit log");

        // First add
        User admin = getUserRepository().findUserByUsername("admin");
        AddGoalToGoalContainerCommand addCmd =
                getProjectCommandFactory().newAddGoalToGoalContainerCommand();
        addCmd.setEditedBy(admin);
        addCmd.setGoal(goal);
        addCmd.setGoalContainer(story);
        addCmd = getCommandHandler().execute(addCmd);
        Story withGoal = (Story) addCmd.getGoalContainer();
        assertFalse(withGoal.getGoals().isEmpty(), "goal should be present after add");

        // Then remove
        RemoveGoalFromGoalContainerCommand removeCmd =
                getProjectCommandFactory().newRemoveGoalFromGoalContainerCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setGoal(goal);
        removeCmd.setGoalContainer(withGoal);
        removeCmd = getCommandHandler().execute(removeCmd);

        Story withoutGoal = (Story) removeCmd.getGoalContainer();
        assertTrue(withoutGoal.getGoals().isEmpty(),
                "goal should be absent from story goals after remove");
    }
}
