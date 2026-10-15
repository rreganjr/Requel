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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddStoryToStoryContainerCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for story-container relationship commands:
 * {@link AddStoryToStoryContainerCommand} and {@link RemoveStoryFromStoryContainerCommand}.
 *
 * Stories are scoped to a project via {@code EditStoryCommand.setStoryContainer(project)}.
 * The add/remove container commands link that story into secondary containers —
 * use cases and actors — which track the association in separate join tables.
 *
 * Project is intentionally excluded: project-level story membership is managed
 * by {@code EditStoryCommand} (the story's {@code projectOrDomain} FK), not by this
 * command pair.
 */
public class StoryContainerCommandTest extends AbstractIntegrationTestCase {

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
        cmd.setOrganizationName("StoryContainerTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("An actor for story container tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private UseCase createUseCase(Project project, Actor primaryActor, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("A use case for story container tests.");
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
        cmd.setText("A story for container relationship tests.");
        cmd.setStoryTypeName(StoryType.Success.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    // -------------------------------------------------------------------------
    // UseCase as container
    // -------------------------------------------------------------------------

    @Test
    public void addStoryToUseCase() throws Exception {
        Project project = createProject("StoryContainer-UC-add");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Manage user account");
        Story story = createStory(project, "Admin creates account for new user");

        assertTrue(useCase.getStories().isEmpty(),
                "use case should have no stories before the command");

        AddStoryToStoryContainerCommand cmd =
                getProjectCommandFactory().newAddStoryToStoryContainerCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setStory(story);
        cmd.setStoryContainer(useCase);
        cmd = getCommandHandler().execute(cmd);

        UseCase updated = (UseCase) cmd.getStoryContainer();
        assertTrue(updated.getStories().stream()
                        .anyMatch(s -> s.getName().equals("Admin creates account for new user")),
                "story should appear in use case stories after add");
    }

    @Test
    public void removeStoryFromUseCase() throws Exception {
        Project project = createProject("StoryContainer-UC-remove");
        Actor actor = createActor(project, "Any User");
        UseCase useCase = createUseCase(project, actor, "Reset credentials");
        Story story = createStory(project, "User requests password reset");

        // First add the story
        User admin = getUserRepository().findUserByUsername("admin");
        AddStoryToStoryContainerCommand addCmd =
                getProjectCommandFactory().newAddStoryToStoryContainerCommand();
        addCmd.setEditedBy(admin);
        addCmd.setStory(story);
        addCmd.setStoryContainer(useCase);
        addCmd = getCommandHandler().execute(addCmd);
        UseCase withStory = (UseCase) addCmd.getStoryContainer();
        assertFalse(withStory.getStories().isEmpty(), "story should be present after add");

        // Now remove it
        RemoveStoryFromStoryContainerCommand removeCmd =
                getProjectCommandFactory().newRemoveStoryFromStoryContainerCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setStory(story);
        removeCmd.setStoryContainer(withStory);
        removeCmd = getCommandHandler().execute(removeCmd);

        UseCase withoutStory = (UseCase) removeCmd.getStoryContainer();
        assertTrue(withoutStory.getStories().isEmpty(),
                "story should be absent from use case stories after remove");
    }
}
