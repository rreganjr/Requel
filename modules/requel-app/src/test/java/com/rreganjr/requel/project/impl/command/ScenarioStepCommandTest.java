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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Integration tests for scenario step management commands:
 * {@link EditScenarioStepCommand} and {@link DeleteScenarioStepCommand}.
 *
 * Steps have no uniqueness constraint and no repository find-by-name method.
 * They are associated with scenarios via {@link EditScenarioCommand#setStepCommands}.
 * Post-delete verification goes through the owning scenario's step collection.
 */
public class ScenarioStepCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("StepTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Scenario createScenarioWithStep(Project project, String scenarioName,
			EditScenarioStepCommand stepCmd) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioCommand scenarioCmd = getProjectCommandFactory().newEditScenarioCommand();
		scenarioCmd.setEditedBy(admin);
		scenarioCmd.setProjectOrDomain(project);
		scenarioCmd.setName(scenarioName);
		scenarioCmd.setText("Scenario for step tests.");
		scenarioCmd.setScenarioTypeName(ScenarioType.Primary.name());
		scenarioCmd.setStepCommands(List.of(stepCmd));
		scenarioCmd = getCommandHandler().execute(scenarioCmd);
		return scenarioCmd.getScenario();
	}

	// -------------------------------------------------------------------------
	// EditScenarioStepCommand
	// -------------------------------------------------------------------------

	@Test
	public void createStep() throws Exception {
		Project project = createProject("Step-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("The user submits the form");
		cmd.setText("The user fills in all required fields and clicks Submit.");
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd = getCommandHandler().execute(cmd);

		Step step = cmd.getStep();
		assertNotNull(step, "step should have been created");
		assertEquals("The user submits the form", step.getName(), "step name should match");
		assertEquals("The user fills in all required fields and clicks Submit.", step.getText(),
				"step text should match");
		assertEquals(ScenarioType.Primary, step.getType(), "step type should be Primary");
	}

	@Test
	public void editStep() throws Exception {
		Project project = createProject("Step-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand createCmd = getProjectCommandFactory().newEditScenarioStepCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("The system validates credentials");
		createCmd.setText("Original validation description.");
		createCmd.setScenarioTypeName(ScenarioType.Primary.name());
		createCmd = getCommandHandler().execute(createCmd);
		Step original = createCmd.getStep();

		EditScenarioStepCommand editCmd = getProjectCommandFactory().newEditScenarioStepCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setStep(original);
		editCmd.setName("The system validates credentials");
		editCmd.setText("The system checks the username and password against stored credentials.");
		editCmd.setScenarioTypeName(ScenarioType.Primary.name());
		editCmd = getCommandHandler().execute(editCmd);

		Step updated = editCmd.getStep();
		assertEquals("The system validates credentials", updated.getName(), "name should be unchanged");
		assertEquals("The system checks the username and password against stored credentials.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void createAlternativeStep() throws Exception {
		Project project = createProject("Step-create-alt");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("The user selects an existing address");
		cmd.setText("Instead of typing a new address, the user picks from their saved addresses.");
		cmd.setScenarioTypeName(ScenarioType.Alternative.name());
		cmd = getCommandHandler().execute(cmd);

		Step step = cmd.getStep();
		assertNotNull(step, "step should have been created");
		assertEquals(ScenarioType.Alternative, step.getType(), "step type should be Alternative");
	}

	// -------------------------------------------------------------------------
	// DeleteScenarioStepCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteStep() throws Exception {
		Project project = createProject("Step-delete");
		User admin = getUserRepository().findUserByUsername("admin");

		// Build the step command — it will be executed as part of the scenario command
		EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
		stepCmd.setEditedBy(admin);
		stepCmd.setProjectOrDomain(project);
		stepCmd.setName("The user clicks confirm");
		stepCmd.setText("The user confirms the action by clicking the Confirm button.");
		stepCmd.setScenarioTypeName(ScenarioType.Primary.name());

		Scenario scenario = createScenarioWithStep(project, "Confirmation scenario", stepCmd);
		Step step = stepCmd.getStep();
		assertNotNull(step, "pre-condition: step should have been created with the scenario");
		assertFalse(scenario.getSteps().isEmpty(),
				"pre-condition: scenario should have at least one step");

		DeleteScenarioStepCommand deleteCmd = getProjectCommandFactory().newDeleteScenarioStepCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setScenarioStep(step);
		getCommandHandler().execute(deleteCmd);

		// Reload the scenario and verify the step is gone
		Scenario reloaded = getProjectRepository()
				.findScenarioByProjectOrDomainAndName(project, "Confirmation scenario");
		assertTrue(reloaded.getSteps().isEmpty(),
				"deleted step should no longer appear in the scenario's step list");
	}
}
