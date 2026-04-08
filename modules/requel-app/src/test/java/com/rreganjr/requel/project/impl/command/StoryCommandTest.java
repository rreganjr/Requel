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
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for story management commands:
 * {@link EditStoryCommand} and {@link DeleteStoryCommand}.
 */
public class StoryCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("StoryTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Actor createActor(Project project, String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText("Actor for story tests.");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getActor();
	}

	// -------------------------------------------------------------------------
	// EditStoryCommand
	// -------------------------------------------------------------------------

	@Test
	public void createSuccessStory() throws Exception {
		Project project = createProject("Story-create-success");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		// Project implements StoryContainer — passing it sets the parent and
		// adds the story to the project's story list.
		cmd.setStoryContainer(project);
		cmd.setName("Eric creates a user account");
		cmd.setText("Eric logs into the system and creates a new account for Rich.");
		cmd.setStoryTypeName(StoryType.Success.name());
		cmd = getCommandHandler().execute(cmd);

		Story story = cmd.getStory();
		assertNotNull(story, "story should have been created");
		assertEquals("Eric creates a user account", story.getName(), "story name should match");
		assertEquals(StoryType.Success, story.getStoryType(), "story type should be Success");
		assertDoesNotThrow(
				() -> getProjectRepository().findStoryByProjectOrDomainAndName(
						project, "Eric creates a user account"),
				"newly created story must be findable on the project");
	}

	@Test
	public void createExceptionStory() throws Exception {
		Project project = createProject("Story-create-exception");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName("Ron enters wrong password");
		cmd.setText("Ron enters his username and an incorrect password. The system displays an error.");
		cmd.setStoryTypeName(StoryType.Exception.name());
		cmd = getCommandHandler().execute(cmd);

		Story story = cmd.getStory();
		assertNotNull(story, "story should have been created");
		assertEquals(StoryType.Exception, story.getStoryType(), "story type should be Exception");
	}

	@Test
	public void createStoryWithPrimaryActor() throws Exception {
		Project project = createProject("Story-create-actor");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Theresa");

		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName("Theresa reviews requirements");
		cmd.setText("Theresa opens the project and reviews the existing goals.");
		cmd.setStoryTypeName(StoryType.Success.name());
		cmd.setPrimaryActorName(actor.getName());
		cmd = getCommandHandler().execute(cmd);

		Story story = cmd.getStory();
		assertNotNull(story.getPrimaryActor(), "story should have a primary actor");
		assertEquals(actor.getName(), story.getPrimaryActor().getName(),
				"primary actor name should match");
	}

	@Test
	public void editStory() throws Exception {
		Project project = createProject("Story-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand createCmd = getProjectCommandFactory().newEditStoryCommand();
		createCmd.setEditedBy(admin);
		createCmd.setStoryContainer(project);
		createCmd.setName("Rich creates a project");
		createCmd.setText("Rich logs in and creates a new project.");
		createCmd.setStoryTypeName(StoryType.Success.name());
		createCmd = getCommandHandler().execute(createCmd);
		Story original = createCmd.getStory();

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setName("Rich creates a project");
		editCmd.setText("Rich logs in, chooses 'New Project', and enters a name and customer.");
		editCmd.setStoryTypeName(StoryType.Success.name());
		editCmd = getCommandHandler().execute(editCmd);

		Story updated = editCmd.getStory();
		assertEquals("Rich creates a project", updated.getName(), "name should be unchanged");
		assertEquals("Rich logs in, chooses 'New Project', and enters a name and customer.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void duplicateStoryNameIsRejected() throws Exception {
		Project project = createProject("Story-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand first = getProjectCommandFactory().newEditStoryCommand();
		first.setEditedBy(admin);
		first.setStoryContainer(project);
		first.setName("Login success");
		first.setText("First definition.");
		first.setStoryTypeName(StoryType.Success.name());
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditStoryCommand dup = getProjectCommandFactory().newEditStoryCommand();
			dup.setEditedBy(admin);
			dup.setStoryContainer(project);
			dup.setName("Login success");
			dup.setText("Duplicate definition.");
			dup.setStoryTypeName(StoryType.Success.name());
			getCommandHandler().execute(dup);
		}, "duplicate story name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteStoryCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteStory() throws Exception {
		Project project = createProject("Story-delete");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand createCmd = getProjectCommandFactory().newEditStoryCommand();
		createCmd.setEditedBy(admin);
		createCmd.setStoryContainer(project);
		createCmd.setName("ToDelete");
		createCmd.setText("This story will be deleted.");
		createCmd.setStoryTypeName(StoryType.Success.name());
		createCmd = getCommandHandler().execute(createCmd);
		Story story = createCmd.getStory();

		DeleteStoryCommand deleteCmd = getProjectCommandFactory().newDeleteStoryCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setStory(story);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findStoryByProjectOrDomainAndName(project, "ToDelete"),
				"deleted story should no longer be findable");
	}
}
