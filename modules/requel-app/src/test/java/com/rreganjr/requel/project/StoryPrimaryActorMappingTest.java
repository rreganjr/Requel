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
package com.rreganjr.requel.project;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.*;
import com.rreganjr.requel.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the {@code primary_actor_id} FK on Story and UseCase.
 *
 * This mapping was added as part of the V2.0 migration. These tests verify that
 * the {@code primary_actor_id} column is persisted and reloaded correctly via a
 * fresh repository lookup — i.e., that Hibernate's JPA mapping for the
 * {@code @ManyToOne(nullable=true)} column on {@code StoryImpl} and
 * {@code UseCaseImpl} survives a full command → persist → fetch round-trip.
 *
 * Scenarios covered:
 * - Story created with primaryActor: reloaded story has correct primaryActor
 * - Story created without primaryActor: reloaded story has null primaryActor
 * - Story primaryActor cleared via edit: reloaded story has null primaryActor
 * - UseCase created with primaryActor: reloaded use case has correct primaryActor
 */
public class StoryPrimaryActorMappingTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Project createProject(String label) throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(label + "-" + ts);
        cmd.setText("primary actor mapping test");
        cmd.setOrganizationName("PrimaryActorOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Actor createActor(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
        cmd.setEditedBy(admin);
        cmd.setActorContainer(project);
        cmd.setName(name);
        cmd.setText("test actor");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getActor();
    }

    private Story createStory(Project project, String name, String primaryActorName)
            throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
        cmd.setEditedBy(admin);
        cmd.setStoryContainer(project);
        cmd.setName(name);
        cmd.setText("test story");
        cmd.setStoryTypeName(StoryType.Success.name());
        if (primaryActorName != null) {
            cmd.setPrimaryActorName(primaryActorName);
        }
        cmd = getCommandHandler().execute(cmd);
        return cmd.getStory();
    }

    private Story reloadStory(Project project, String name) throws Exception {
        return getProjectRepository().findStoryByProjectOrDomainAndName(project, name);
    }

    // -------------------------------------------------------------------------
    // Story primary actor
    // -------------------------------------------------------------------------

    @Test
    void storyWithPrimaryActorSurvivesRoundTrip() throws Exception {
        Project project = createProject("Story-primary-actor");
        createActor(project, "Theresa");
        createStory(project, "Login story", "Theresa");

        Story reloaded = reloadStory(project, "Login story");

        assertNotNull(reloaded.getPrimaryActor(),
                "expected primaryActor to be persisted");
        assertEquals("Theresa", reloaded.getPrimaryActor().getName());
    }

    @Test
    void storyWithoutPrimaryActorHasNullOnReload() throws Exception {
        Project project = createProject("Story-no-primary-actor");
        createStory(project, "No actor story", null);

        Story reloaded = reloadStory(project, "No actor story");

        assertNull(reloaded.getPrimaryActor(),
                "expected null primaryActor when none was set");
    }

    @Test
    void clearingPrimaryActorViaEditPersistsNull() throws Exception {
        Project project = createProject("Story-clear-actor");
        createActor(project, "Ron");
        createStory(project, "Story with actor", "Ron");

        // Edit the same story and clear the primary actor
        User admin = getUserRepository().findUserByUsername("admin");
        Story existing = reloadStory(project, "Story with actor");
        EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
        editCmd.setEditedBy(admin);
        editCmd.setStoryContainer(project);
        editCmd.setStory(existing);
        editCmd.setName("Story with actor");
        editCmd.setText("updated text");
        editCmd.setStoryTypeName(StoryType.Success.name());
        // not setting primaryActorName → clears it
        getCommandHandler().execute(editCmd);

        Story reloaded = reloadStory(project, "Story with actor");

        assertNull(reloaded.getPrimaryActor(),
                "expected null primaryActor after clearing via edit");
    }

    // -------------------------------------------------------------------------
    // UseCase primary actor
    // -------------------------------------------------------------------------

    @Test
    void useCaseWithPrimaryActorSurvivesRoundTrip() throws Exception {
        Project project = createProject("UseCase-primary-actor");
        createActor(project, "Jason");

        User admin = getUserRepository().findUserByUsername("admin");
        EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName("Login use case");
        cmd.setText("test use case");
        cmd.setPrimaryActorName("Jason");
        cmd = getCommandHandler().execute(cmd);

        UseCase reloaded = getProjectRepository()
                .findUseCaseByProjectOrDomainAndName(project, "Login use case");

        assertNotNull(reloaded.getPrimaryActor(),
                "expected primaryActor to be persisted on UseCase");
        assertEquals("Jason", reloaded.getPrimaryActor().getName());
    }
}
