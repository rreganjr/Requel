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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for scenario management commands:
 * {@link EditScenarioCommand} and {@link DeleteScenarioCommand}.
 *
 * Scenarios are standalone project entities linked directly via
 * setProjectOrDomain — there is no container pattern. A use case's primary
 * scenario is auto-created by EditUseCaseCommand; tests here create independent
 * scenarios to avoid coupling to use case state.
 */
public class ScenarioCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("ScenarioTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	// -------------------------------------------------------------------------
	// EditScenarioCommand
	// -------------------------------------------------------------------------

	@Test
	public void createPrimaryScenario() throws Exception {
		Project project = createProject("Scenario-create-primary");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("Login scenario");
		cmd.setText("The user enters credentials and the system validates them.");
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd = getCommandHandler().execute(cmd);

		Scenario scenario = cmd.getScenario();
		assertNotNull(scenario, "scenario should have been created");
		assertEquals("Login scenario", scenario.getName(), "scenario name should match");
		assertEquals(ScenarioType.Primary, scenario.getType(), "scenario type should be Primary");
		assertDoesNotThrow(
				() -> getProjectRepository().findScenarioByProjectOrDomainAndName(
						project, "Login scenario"),
				"newly created scenario must be findable on the project");
	}

	@Test
	public void createAlternativeScenario() throws Exception {
		Project project = createProject("Scenario-create-alt");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("Select existing customer");
		cmd.setText("The user selects a customer from the existing list instead of typing a new name.");
		cmd.setScenarioTypeName(ScenarioType.Alternative.name());
		cmd = getCommandHandler().execute(cmd);

		Scenario scenario = cmd.getScenario();
		assertNotNull(scenario, "scenario should have been created");
		assertEquals(ScenarioType.Alternative, scenario.getType(), "scenario type should be Alternative");
	}

	@Test
	public void createExceptionScenario() throws Exception {
		Project project = createProject("Scenario-create-exception");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("Invalid credentials");
		cmd.setText("The system informs the user the username and password combination are not valid.");
		cmd.setScenarioTypeName(ScenarioType.Exception.name());
		cmd = getCommandHandler().execute(cmd);

		Scenario scenario = cmd.getScenario();
		assertNotNull(scenario, "scenario should have been created");
		assertEquals(ScenarioType.Exception, scenario.getType(), "scenario type should be Exception");
	}

	@Test
	public void editScenario() throws Exception {
		Project project = createProject("Scenario-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand createCmd = getProjectCommandFactory().newEditScenarioCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Create project scenario");
		createCmd.setText("The user enters a project name and customer.");
		createCmd.setScenarioTypeName(ScenarioType.Primary.name());
		createCmd = getCommandHandler().execute(createCmd);
		Scenario original = createCmd.getScenario();

		EditScenarioCommand editCmd = getProjectCommandFactory().newEditScenarioCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setScenario(original);
		editCmd.setName("Create project scenario");
		editCmd.setText("The user enters a name, customer, and optional description, then submits.");
		editCmd.setScenarioTypeName(ScenarioType.Primary.name());
		editCmd = getCommandHandler().execute(editCmd);

		Scenario updated = editCmd.getScenario();
		assertEquals("Create project scenario", updated.getName(), "name should be unchanged");
		assertEquals("The user enters a name, customer, and optional description, then submits.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void duplicateScenarioNameIsRejected() throws Exception {
		Project project = createProject("Scenario-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand first = getProjectCommandFactory().newEditScenarioCommand();
		first.setEditedBy(admin);
		first.setProjectOrDomain(project);
		first.setName("Stakeholder scenario");
		first.setText("First definition.");
		first.setScenarioTypeName(ScenarioType.Primary.name());
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditScenarioCommand dup = getProjectCommandFactory().newEditScenarioCommand();
			dup.setEditedBy(admin);
			dup.setProjectOrDomain(project);
			dup.setName("Stakeholder scenario");
			dup.setText("Duplicate definition.");
			dup.setScenarioTypeName(ScenarioType.Alternative.name());
			getCommandHandler().execute(dup);
		}, "duplicate scenario name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteScenarioCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteScenario() throws Exception {
		Project project = createProject("Scenario-delete");
		User admin = getUserRepository().findUserByUsername("admin");

		// Use Alternative type to avoid confusing this with a use case's
		// primary scenario (which is managed by EditUseCaseCommand).
		EditScenarioCommand createCmd = getProjectCommandFactory().newEditScenarioCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("ToDelete");
		createCmd.setText("This scenario will be deleted.");
		createCmd.setScenarioTypeName(ScenarioType.Alternative.name());
		createCmd = getCommandHandler().execute(createCmd);
		Scenario scenario = createCmd.getScenario();

		DeleteScenarioCommand deleteCmd = getProjectCommandFactory().newDeleteScenarioCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setScenario(scenario);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findScenarioByProjectOrDomainAndName(project, "ToDelete"),
				"deleted scenario should no longer be findable");
	}
}
