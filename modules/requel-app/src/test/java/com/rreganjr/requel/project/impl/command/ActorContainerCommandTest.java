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
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for actor-container relationship commands:
 * {@link AddActorToActorContainerCommand} and {@link RemoveActorFromActorContainerCommand}.
 *
 * Actors are scoped to a project via {@code EditActorCommand.setActorContainer(project)}.
 * The add/remove container commands link that actor into secondary containers —
 * use cases and stories — which track the association in separate join tables.
 *
 * Project is intentionally excluded: project-level actor membership is managed
 * by {@code EditActorCommand} (mapped via the actor's {@code projectOrDomain} FK),
 * not by this command pair.
 */
public class ActorContainerCommandTest extends AbstractIntegrationTestCase {

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
        cmd.setOrganizationName("ActorContainerTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("An actor for container relationship tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private UseCase createUseCase(Project project, Actor primaryActor, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText("A use case for actor container tests.");
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
        cmd.setText("A story for actor container tests.");
        cmd.setStoryTypeName(StoryType.Success.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    // -------------------------------------------------------------------------
    // UseCase as container
    // -------------------------------------------------------------------------

    @Test
    public void addActorToUseCase() throws Exception {
        Project project = createProject("ActorContainer-UC-add");
        Actor primaryActor = createActor(project, "Primary User");
        Actor additionalActor = createActor(project, "Supporting User");
        UseCase useCase = createUseCase(project, primaryActor, "Log in to the system");

        assertTrue(useCase.getActors().isEmpty(),
                "use case should have no additional actors before the command");

        AddActorToActorContainerCommand cmd =
                getProjectCommandFactory().newAddActorToActorContainerCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setActor(additionalActor);
        cmd.setActorContainer(useCase);
        cmd = getCommandHandler().execute(cmd);

        UseCase updated = (UseCase) cmd.getActorContainer();
        assertTrue(updated.getActors().stream()
                        .anyMatch(a -> a.getName().equals("Supporting User")),
                "actor should appear in use case actors after add");
    }

    @Test
    public void removeActorFromUseCase() throws Exception {
        Project project = createProject("ActorContainer-UC-remove");
        Actor primaryActor = createActor(project, "Primary User");
        Actor actor = createActor(project, "Removable User");
        UseCase useCase = createUseCase(project, primaryActor, "Manage account");

        // First add the actor
        User admin = getUserRepository().findUserByUsername("admin");
        AddActorToActorContainerCommand addCmd =
                getProjectCommandFactory().newAddActorToActorContainerCommand();
        addCmd.setEditedBy(admin);
        addCmd.setActor(actor);
        addCmd.setActorContainer(useCase);
        addCmd = getCommandHandler().execute(addCmd);
        UseCase withActor = (UseCase) addCmd.getActorContainer();
        assertFalse(withActor.getActors().isEmpty(),
                "actor should be present after add");

        // Now remove it
        RemoveActorFromActorContainerCommand removeCmd =
                getProjectCommandFactory().newRemoveActorFromActorContainerCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setActor(actor);
        removeCmd.setActorContainer(withActor);
        removeCmd = getCommandHandler().execute(removeCmd);

        UseCase withoutActor = (UseCase) removeCmd.getActorContainer();
        assertTrue(withoutActor.getActors().isEmpty(),
                "actor should be absent from use case actors after remove");
    }

    // -------------------------------------------------------------------------
    // Story as container
    // -------------------------------------------------------------------------

    @Test
    public void addActorToStory() throws Exception {
        Project project = createProject("ActorContainer-Story-add");
        Actor actor = createActor(project, "Supporting Actor");
        Story story = createStory(project, "User resets password");

        assertTrue(story.getActors().isEmpty(),
                "story should have no actors before the command");

        AddActorToActorContainerCommand cmd =
                getProjectCommandFactory().newAddActorToActorContainerCommand();
        User admin = getUserRepository().findUserByUsername("admin");
        cmd.setEditedBy(admin);
        cmd.setActor(actor);
        cmd.setActorContainer(story);
        cmd = getCommandHandler().execute(cmd);

        Story updated = (Story) cmd.getActorContainer();
        assertTrue(updated.getActors().stream()
                        .anyMatch(a -> a.getName().equals("Supporting Actor")),
                "actor should appear in story actors after add");
    }

    @Test
    public void removeActorFromStory() throws Exception {
        Project project = createProject("ActorContainer-Story-remove");
        Actor actor = createActor(project, "Story Actor");
        Story story = createStory(project, "Admin resets account");

        // First add
        User admin = getUserRepository().findUserByUsername("admin");
        AddActorToActorContainerCommand addCmd =
                getProjectCommandFactory().newAddActorToActorContainerCommand();
        addCmd.setEditedBy(admin);
        addCmd.setActor(actor);
        addCmd.setActorContainer(story);
        addCmd = getCommandHandler().execute(addCmd);
        Story withActor = (Story) addCmd.getActorContainer();
        assertFalse(withActor.getActors().isEmpty(),
                "actor should be present after add");

        // Then remove
        RemoveActorFromActorContainerCommand removeCmd =
                getProjectCommandFactory().newRemoveActorFromActorContainerCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setActor(actor);
        removeCmd.setActorContainer(withActor);
        removeCmd = getCommandHandler().execute(removeCmd);

        Story withoutActor = (Story) removeCmd.getActorContainer();
        assertTrue(withoutActor.getActors().isEmpty(),
                "actor should be absent from story actors after remove");
    }
}
