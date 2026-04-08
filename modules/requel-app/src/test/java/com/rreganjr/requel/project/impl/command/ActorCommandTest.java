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
import com.rreganjr.requel.project.command.DeleteActorCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for actor management commands:
 * {@link EditActorCommand} and {@link DeleteActorCommand}.
 */
public class ActorCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("ActorTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	// -------------------------------------------------------------------------
	// EditActorCommand
	// -------------------------------------------------------------------------

	@Test
	public void createActor() throws Exception {
		Project project = createProject("Actor-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		// Project implements ActorContainer — passing it here sets the parent
		// project and adds the actor to the project's actor list.
		cmd.setActorContainer(project);
		cmd.setName("Administrator");
		cmd.setText("Manages user accounts and system configuration.");
		cmd = getCommandHandler().execute(cmd);

		Actor actor = cmd.getActor();
		assertNotNull(actor, "actor should have been created");
		assertEquals("Administrator", actor.getName(), "actor name should match");
		assertEquals("Manages user accounts and system configuration.", actor.getText(),
				"actor text should match");
		assertDoesNotThrow(
				() -> getProjectRepository().findActorByProjectOrDomainAndName(project, "Administrator"),
				"newly created actor must be findable on the project");
	}

	@Test
	public void editActor() throws Exception {
		Project project = createProject("Actor-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditActorCommand createCmd = getProjectCommandFactory().newEditActorCommand();
		createCmd.setEditedBy(admin);
		createCmd.setActorContainer(project);
		createCmd.setName("Project User");
		createCmd.setText("Can create and edit requirements on one or more projects.");
		createCmd = getCommandHandler().execute(createCmd);
		Actor original = createCmd.getActor();

		EditActorCommand editCmd = getProjectCommandFactory().newEditActorCommand();
		editCmd.setEditedBy(admin);
		editCmd.setActor(original);
		editCmd.setName("Project User");
		editCmd.setText("Creates requirements and participates in project discussions.");
		editCmd = getCommandHandler().execute(editCmd);

		Actor updated = editCmd.getActor();
		assertEquals("Project User", updated.getName(), "name should be unchanged");
		assertEquals("Creates requirements and participates in project discussions.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void duplicateActorNameIsRejected() throws Exception {
		Project project = createProject("Actor-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditActorCommand first = getProjectCommandFactory().newEditActorCommand();
		first.setEditedBy(admin);
		first.setActorContainer(project);
		first.setName("Analyst");
		first.setText("First definition.");
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditActorCommand dup = getProjectCommandFactory().newEditActorCommand();
			dup.setEditedBy(admin);
			dup.setActorContainer(project);
			dup.setName("Analyst");
			dup.setText("Duplicate definition.");
			getCommandHandler().execute(dup);
		}, "duplicate actor name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteActorCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteActor() throws Exception {
		Project project = createProject("Actor-delete");
		User admin = getUserRepository().findUserByUsername("admin");

		EditActorCommand createCmd = getProjectCommandFactory().newEditActorCommand();
		createCmd.setEditedBy(admin);
		createCmd.setActorContainer(project);
		createCmd.setName("ToDelete");
		createCmd.setText("This actor will be deleted.");
		createCmd = getCommandHandler().execute(createCmd);
		Actor actor = createCmd.getActor();

		DeleteActorCommand deleteCmd = getProjectCommandFactory().newDeleteActorCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setActor(actor);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findActorByProjectOrDomainAndName(project, "ToDelete"),
				"deleted actor should no longer be findable");
	}
}
