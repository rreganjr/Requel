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
import com.rreganjr.platform.exception.EntityLockException;
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
import com.rreganjr.validator.EntityValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
	public void editStoryWithMatchingVersionSucceeds() throws Exception {
		Project project = createProject("Story-version-ok");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand createCmd = getProjectCommandFactory().newEditStoryCommand();
		createCmd.setEditedBy(admin);
		createCmd.setStoryContainer(project);
		createCmd.setName("Versioned story");
		createCmd.setText("Initial text.");
		createCmd.setStoryTypeName(StoryType.Success.name());
		createCmd = getCommandHandler().execute(createCmd);
		Story original = createCmd.getStory();

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setName("Versioned story");
		editCmd.setText("Updated with the current version.");
		editCmd.setStoryTypeName(StoryType.Success.name());
		editCmd.setExpectedVersion(original.getVersion());
		editCmd = getCommandHandler().execute(editCmd);

		assertEquals("Updated with the current version.", editCmd.getStory().getText(),
				"matching-version update should be applied");
	}

	@Test
	public void editStoryWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("Story-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand createCmd = getProjectCommandFactory().newEditStoryCommand();
		createCmd.setEditedBy(admin);
		createCmd.setStoryContainer(project);
		createCmd.setName("Contested story");
		createCmd.setText("Initial text.");
		createCmd.setStoryTypeName(StoryType.Success.name());
		createCmd = getCommandHandler().execute(createCmd);
		Story original = createCmd.getStory();
		int staleVersion = original.getVersion();

		EditStoryCommand firstEdit = getProjectCommandFactory().newEditStoryCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setStory(original);
		firstEdit.setName("Contested story");
		firstEdit.setText("First writer's change.");
		firstEdit.setStoryTypeName(StoryType.Success.name());
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditStoryCommand staleEdit = getProjectCommandFactory().newEditStoryCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setStory(original);
			staleEdit.setName("Contested story");
			staleEdit.setText("Second writer's stale change.");
			staleEdit.setStoryTypeName(StoryType.Success.name());
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
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

	// -------------------------------------------------------------------------
	// Partial update (issue #316): null leaves a property as it is, "" clears the text
	// -------------------------------------------------------------------------

	private Story createStory(Project project, String name, String text, StoryType type)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setStoryTypeName(type.name());
		cmd = getCommandHandler().execute(cmd);
		return cmd.getStory();
	}

	@Test
	public void editStoryWithNullTextAndTypeKeepsThem() throws Exception {
		Project project = createProject("Story-partial");
		User admin = getUserRepository().findUserByUsername("admin");
		Story original = createStory(project, "Ron forgets his password",
				"Ron asks for a reset link.", StoryType.Exception);

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setName("Ron resets his password");
		editCmd = getCommandHandler().execute(editCmd);

		Story updated = getProjectRepository().get(editCmd.getStory());
		assertEquals("Ron resets his password", updated.getName(),
				"the supplied name should be applied");
		assertEquals("Ron asks for a reset link.", updated.getText(),
				"a null text should leave the text as it is");
		assertEquals(StoryType.Exception, updated.getStoryType(),
				"a null story type should leave the type as it is");
	}

	@Test
	public void editStoryWithEmptyTextClearsText() throws Exception {
		Project project = createProject("Story-clear-text");
		User admin = getUserRepository().findUserByUsername("admin");
		Story original = createStory(project, "Rich exports a report",
				"Rich downloads the HTML report.", StoryType.Success);

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setName("Rich exports a report");
		editCmd.setText("");
		editCmd.setStoryTypeName(StoryType.Success.name());
		editCmd = getCommandHandler().execute(editCmd);

		Story updated = getProjectRepository().get(editCmd.getStory());
		assertEquals("", updated.getText(), "an empty text should clear the text");
	}

	// -------------------------------------------------------------------------
	// Primary actor on update (issue #325): null leaves it, "" clears it, an unknown name is
	// refused
	// -------------------------------------------------------------------------

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Story createStoryWithActor(Project project, String name, Actor actor) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName(name);
		cmd.setText("Story for primary-actor tests.");
		cmd.setStoryTypeName(StoryType.Success.name());
		cmd.setPrimaryActorName(actor.getName());
		cmd = getCommandHandler().execute(cmd);
		return cmd.getStory();
	}

	private int actorRefererRows(Actor actor, Story story) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM actor_actorcontainers WHERE actor_id = ? AND actorcontainer_id = ?",
				Integer.class, actor.getId(), story.getId());
	}

	@Test
	public void editStoryWithoutPrimaryActorNameKeepsTheActor() throws Exception {
		Project project = createProject("Story-actor-keep");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Cashier");
		Story original = createStoryWithActor(project, "Cashier opens the till", actor);

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setText("The cashier counts the float and opens the till.");
		editCmd = getCommandHandler().execute(editCmd);

		Story updated = getProjectRepository().get(editCmd.getStory());
		assertEquals("The cashier counts the float and opens the till.", updated.getText(),
				"the supplied text should be applied");
		assertNotNull(updated.getPrimaryActor(), "a null primaryActorName should keep the actor");
		assertEquals(actor.getName(), updated.getPrimaryActor().getName());
		assertEquals(1, actorRefererRows(actor, updated), "the actor should still list the story");
	}

	@Test
	public void editStoryWithEmptyPrimaryActorNameClearsIt() throws Exception {
		Project project = createProject("Story-actor-clear");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Courier");
		Story original = createStoryWithActor(project, "Courier delivers a parcel", actor);
		assertEquals(1, actorRefererRows(actor, original), "fixture: the actor lists the story");

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setPrimaryActorName("");
		editCmd = getCommandHandler().execute(editCmd);

		Story updated = getProjectRepository().get(editCmd.getStory());
		assertNull(updated.getPrimaryActor(), "an empty primaryActorName should clear the actor");
		assertEquals(0, actorRefererRows(actor, updated),
				"clearing should remove the story from the actor's referers");
	}

	@Test
	public void editStoryWithUnknownPrimaryActorIsRefusedAndKeepsTheActor() throws Exception {
		Project project = createProject("Story-actor-unknown");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Pilot");
		Story original = createStoryWithActor(project, "Pilot files a flight plan", actor);

		EditStoryCommand editCmd = getProjectCommandFactory().newEditStoryCommand();
		editCmd.setEditedBy(admin);
		editCmd.setStory(original);
		editCmd.setPrimaryActorName("Nobody by this name");
		EntityValidationException e = assertThrows(EntityValidationException.class,
				() -> getCommandHandler().execute(editCmd));
		assertArrayEquals(new String[] { "primaryActorName" }, e.getEntityPropertyNames(),
				"the refusal should name the primaryActorName field");

		Story reloaded = getProjectRepository().findStoryByProjectOrDomainAndName(project,
				"Pilot files a flight plan");
		assertNotNull(reloaded.getPrimaryActor(), "a refused edit must leave the actor alone");
		assertEquals(actor.getName(), reloaded.getPrimaryActor().getName());
	}

	@Test
	public void createStoryWithUnknownPrimaryActorIsRefused() throws Exception {
		Project project = createProject("Story-actor-unknown-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditStoryCommand cmd = getProjectCommandFactory().newEditStoryCommand();
		cmd.setEditedBy(admin);
		cmd.setStoryContainer(project);
		cmd.setName("Ghost reviews the backlog");
		cmd.setStoryTypeName(StoryType.Success.name());
		cmd.setPrimaryActorName("Ghost");
		assertThrows(EntityValidationException.class, () -> getCommandHandler().execute(cmd));
		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findStoryByProjectOrDomainAndName(project,
						"Ghost reviews the backlog"),
				"a refused create must not leave a story behind");
	}
}
