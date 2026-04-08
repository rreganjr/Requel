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
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for use case management commands:
 * {@link EditUseCaseCommand} and {@link DeleteUseCaseCommand}.
 *
 * Unlike Goal/Actor/Story, creating a use case also auto-creates a primary
 * scenario (EditScenarioCommand is invoked internally). setProjectOrDomain()
 * is required — there is no container pattern for use cases.
 */
public class UseCaseCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("UseCaseTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Actor createActor(Project project, String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditActorCommand cmd = getProjectCommandFactory().newEditActorCommand();
		cmd.setEditedBy(admin);
		cmd.setActorContainer(project);
		cmd.setName(name);
		cmd.setText("Actor for use case tests.");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getActor();
	}

	// -------------------------------------------------------------------------
	// EditUseCaseCommand
	// -------------------------------------------------------------------------

	@Test
	public void createUseCase() throws Exception {
		Project project = createProject("UseCase-create");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Any User");

		EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
		cmd.setEditedBy(admin);
		// setProjectOrDomain is required — use cases have no container pattern
		cmd.setProjectOrDomain(project);
		cmd.setName("Login to the system");
		cmd.setText("All users must be authenticated with a username and password.");
		cmd.setPrimaryActorName(actor.getName());
		cmd = getCommandHandler().execute(cmd);

		UseCase useCase = cmd.getUseCase();
		assertNotNull(useCase, "use case should have been created");
		assertEquals("Login to the system", useCase.getName(), "use case name should match");
		assertNotNull(useCase.getScenario(),
				"creating a use case must also create a primary scenario");
		assertNotNull(useCase.getPrimaryActor(), "use case should have a primary actor");
		assertEquals(actor.getName(), useCase.getPrimaryActor().getName(),
				"primary actor name should match");
		assertDoesNotThrow(
				() -> getProjectRepository().findUseCaseByProjectOrDomainAndName(
						project, "Login to the system"),
				"newly created use case must be findable on the project");
	}

	@Test
	public void createUseCaseAutoCreatesUnknownActor() throws Exception {
		Project project = createProject("UseCase-autocreate-actor");
		User admin = getUserRepository().findUserByUsername("admin");

		EditUseCaseCommand cmd = getProjectCommandFactory().newEditUseCaseCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("Create a new project");
		cmd.setText("An authorized user creates a new requirements project.");
		// Actor does not exist yet — EditUseCaseCommandImpl auto-creates it
		cmd.setPrimaryActorName("Project User");
		cmd = getCommandHandler().execute(cmd);

		UseCase useCase = cmd.getUseCase();
		assertNotNull(useCase.getPrimaryActor(), "use case should have a primary actor");
		assertEquals("Project User", useCase.getPrimaryActor().getName(),
				"auto-created actor name should match");
		// Verify the actor was also persisted on the project
		assertDoesNotThrow(
				() -> getProjectRepository().findActorByProjectOrDomainAndName(project, "Project User"),
				"auto-created actor must be findable on the project");
	}

	@Test
	public void editUseCase() throws Exception {
		Project project = createProject("UseCase-edit");
		User admin = getUserRepository().findUserByUsername("admin");
		Actor actor = createActor(project, "Administrator");

		EditUseCaseCommand createCmd = getProjectCommandFactory().newEditUseCaseCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Create or edit a user account");
		createCmd.setText("An administrator creates user accounts.");
		createCmd.setPrimaryActorName(actor.getName());
		createCmd = getCommandHandler().execute(createCmd);
		UseCase original = createCmd.getUseCase();

		EditUseCaseCommand editCmd = getProjectCommandFactory().newEditUseCaseCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setUseCase(original);
		editCmd.setName("Create or edit a user account");
		editCmd.setText("An administrator creates and manages user accounts, roles, and permissions.");
		editCmd.setPrimaryActorName(actor.getName());
		editCmd = getCommandHandler().execute(editCmd);

		UseCase updated = editCmd.getUseCase();
		assertEquals("Create or edit a user account", updated.getName(), "name should be unchanged");
		assertEquals("An administrator creates and manages user accounts, roles, and permissions.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void duplicateUseCaseNameIsRejected() throws Exception {
		Project project = createProject("UseCase-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditUseCaseCommand first = getProjectCommandFactory().newEditUseCaseCommand();
		first.setEditedBy(admin);
		first.setProjectOrDomain(project);
		first.setName("Create a stakeholder");
		first.setText("First definition.");
		first.setPrimaryActorName("Analyst");
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditUseCaseCommand dup = getProjectCommandFactory().newEditUseCaseCommand();
			dup.setEditedBy(admin);
			dup.setProjectOrDomain(project);
			dup.setName("Create a stakeholder");
			dup.setText("Duplicate definition.");
			dup.setPrimaryActorName("Analyst");
			getCommandHandler().execute(dup);
		}, "duplicate use case name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteUseCaseCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteUseCase() throws Exception {
		Project project = createProject("UseCase-delete");
		User admin = getUserRepository().findUserByUsername("admin");
		// DeleteUseCaseCommandImpl adds getPrimaryActor() to its actor set,
		// so a use case without a primary actor would NPE on delete.
		Actor actor = createActor(project, "Requester");

		EditUseCaseCommand createCmd = getProjectCommandFactory().newEditUseCaseCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("ToDelete");
		createCmd.setText("This use case will be deleted.");
		createCmd.setPrimaryActorName(actor.getName());
		createCmd = getCommandHandler().execute(createCmd);
		UseCase useCase = createCmd.getUseCase();

		DeleteUseCaseCommand deleteCmd = getProjectCommandFactory().newDeleteUseCaseCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setUseCase(useCase);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findUseCaseByProjectOrDomainAndName(project, "ToDelete"),
				"deleted use case should no longer be findable");
	}
}
