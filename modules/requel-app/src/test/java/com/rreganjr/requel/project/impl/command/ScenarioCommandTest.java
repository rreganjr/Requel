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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.user.User;
import java.util.ArrayList;
import java.util.List;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
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
	public void editScenarioWithMatchingVersionSucceeds() throws Exception {
		Project project = createProject("Scenario-version-ok");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand createCmd = getProjectCommandFactory().newEditScenarioCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Versioned scenario");
		createCmd.setText("Initial text.");
		createCmd.setScenarioTypeName(ScenarioType.Primary.name());
		createCmd = getCommandHandler().execute(createCmd);
		Scenario original = createCmd.getScenario();

		EditScenarioCommand editCmd = getProjectCommandFactory().newEditScenarioCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setScenario(original);
		editCmd.setName("Versioned scenario");
		editCmd.setText("Updated with the current version.");
		editCmd.setScenarioTypeName(ScenarioType.Primary.name());
		editCmd.setExpectedVersion(original.getVersion());
		editCmd = getCommandHandler().execute(editCmd);

		assertEquals("Updated with the current version.", editCmd.getScenario().getText(),
				"matching-version update should be applied");
	}

	@Test
	public void editScenarioWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("Scenario-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioCommand createCmd = getProjectCommandFactory().newEditScenarioCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Contested scenario");
		createCmd.setText("Initial text.");
		createCmd.setScenarioTypeName(ScenarioType.Primary.name());
		createCmd = getCommandHandler().execute(createCmd);
		Scenario original = createCmd.getScenario();
		int staleVersion = original.getVersion();

		EditScenarioCommand firstEdit = getProjectCommandFactory().newEditScenarioCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setProjectOrDomain(project);
		firstEdit.setScenario(original);
		firstEdit.setName("Contested scenario");
		firstEdit.setText("First writer's change.");
		firstEdit.setScenarioTypeName(ScenarioType.Primary.name());
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditScenarioCommand staleEdit = getProjectCommandFactory().newEditScenarioCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setProjectOrDomain(project);
			staleEdit.setScenario(original);
			staleEdit.setName("Contested scenario");
			staleEdit.setText("Second writer's stale change.");
			staleEdit.setScenarioTypeName(ScenarioType.Primary.name());
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
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

	// -------------------------------------------------------------------------
	// Partial update (issue #316): null leaves a property as it is, "" clears the text
	// -------------------------------------------------------------------------

	private Scenario createScenario(Project project, String name, String text, ScenarioType type)
			throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd.setScenarioTypeName(type.name());
		cmd = getCommandHandler().execute(cmd);
		return cmd.getScenario();
	}

	@Test
	public void editScenarioWithNullTextAndTypeKeepsThem() throws Exception {
		Project project = createProject("Scenario-partial");
		User admin = getUserRepository().findUserByUsername("admin");
		Scenario original = createScenario(project, "Pay by card",
				"The user pays with a saved card.", ScenarioType.Alternative);

		EditScenarioCommand editCmd = getProjectCommandFactory().newEditScenarioCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setScenario(original);
		editCmd.setName("Pay by saved card");
		editCmd = getCommandHandler().execute(editCmd);

		Scenario updated = getProjectRepository().get(editCmd.getScenario());
		assertEquals("Pay by saved card", updated.getName(), "the supplied name should be applied");
		assertEquals("The user pays with a saved card.", updated.getText(),
				"a null text should leave the text as it is");
		assertEquals(ScenarioType.Alternative, updated.getType(),
				"a null scenario type should leave the type as it is");
	}

	@Test
	public void editScenarioWithEmptyTextClearsText() throws Exception {
		Project project = createProject("Scenario-clear-text");
		User admin = getUserRepository().findUserByUsername("admin");
		Scenario original = createScenario(project, "Cancel order",
				"The user cancels before shipping.", ScenarioType.Exception);

		EditScenarioCommand editCmd = getProjectCommandFactory().newEditScenarioCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setScenario(original);
		editCmd.setName("Cancel order");
		editCmd.setText("");
		editCmd = getCommandHandler().execute(editCmd);

		Scenario updated = getProjectRepository().get(editCmd.getScenario());
		assertEquals("", updated.getText(), "an empty text should clear the text");
		assertEquals(ScenarioType.Exception, updated.getType(), "type should be unchanged");
	}

	// -------------------------------------------------------------------------
	// Step list on update (issue #325): null leaves it, a list replaces it, and a dropped plain
	// step is deleted unless another scenario uses it
	// -------------------------------------------------------------------------

	private EditScenarioStepCommand newStep(Project project, String name) {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioStepCommand step = getProjectCommandFactory().newEditScenarioStepCommand();
		step.setEditedBy(admin);
		step.setProjectOrDomain(project);
		step.setName(name);
		step.setText("A step.");
		step.setScenarioTypeName(ScenarioType.Primary.name());
		return step;
	}

	/** A step command that re-sends an existing step or sub-scenario, as the API does. */
	private EditScenarioStepCommand existingStep(Project project, Step step) {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioStepCommand cmd = getProjectCommandFactory().newEditScenarioStepCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setStep(step);
		return cmd;
	}

	private Scenario createScenarioWithSteps(Project project, String name,
			List<EditScenarioStepCommand> steps) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setScenarioTypeName(ScenarioType.Primary.name());
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getScenario();
	}

	private Scenario replaceSteps(Project project, Scenario scenario,
			List<EditScenarioStepCommand> steps) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditScenarioCommand cmd = getProjectCommandFactory().newEditScenarioCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setScenario(getProjectRepository().get(scenario));
		cmd.setStepCommands(steps);
		return getCommandHandler().execute(cmd).getScenario();
	}

	private boolean stepExists(Project project, String name) {
		try {
			getProjectRepository().findStepByProjectOrDomainAndName(project, name);
			return true;
		} catch (NoSuchEntityException e) {
			return false;
		}
	}

	@Test
	public void editScenarioWithNullStepCommandsKeepsTheSteps() throws Exception {
		Project project = createProject("Scenario-steps-keep");
		User admin = getUserRepository().findUserByUsername("admin");
		String stepName = "Scan the badge " + System.nanoTime();
		Scenario original = createScenarioWithSteps(project, "Enter the building",
				List.of(newStep(project, stepName)));

		EditScenarioCommand editCmd = getProjectCommandFactory().newEditScenarioCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setScenario(original);
		editCmd.setText("The employee badges in at the front door.");
		editCmd = getCommandHandler().execute(editCmd);

		Scenario updated = getProjectRepository().get(editCmd.getScenario());
		assertEquals("The employee badges in at the front door.", updated.getText());
		assertEquals(1, updated.getSteps().size(), "null step commands should keep the steps");
		assertEquals(stepName, updated.getSteps().get(0).getName());
	}

	@Test
	public void editScenarioWithAnEmptyStepListRemovesAndDeletesTheSteps() throws Exception {
		Project project = createProject("Scenario-steps-clear");
		String first = "Open the valve " + System.nanoTime();
		String second = "Close the valve " + System.nanoTime();
		Scenario original = createScenarioWithSteps(project, "Bleed the radiator",
				List.of(newStep(project, first), newStep(project, second)));

		Scenario updated = replaceSteps(project, original, new ArrayList<>());

		assertEquals(0, getProjectRepository().get(updated).getSteps().size(),
				"an empty list should remove every step");
		assertFalse(stepExists(project, first), "a dropped step no scenario uses is deleted");
		assertFalse(stepExists(project, second), "a dropped step no scenario uses is deleted");
	}

	@Test
	public void droppingOneStepDeletesOnlyThatStep() throws Exception {
		Project project = createProject("Scenario-steps-drop-one");
		String kept = "Preheat the oven " + System.nanoTime();
		String dropped = "Grease the tin " + System.nanoTime();
		Scenario original = createScenarioWithSteps(project, "Bake a cake",
				List.of(newStep(project, kept), newStep(project, dropped)));
		Step keptStep = getProjectRepository().findStepByProjectOrDomainAndName(project, kept);

		Scenario updated = replaceSteps(project, original, List.of(existingStep(project, keptStep)));

		updated = getProjectRepository().get(updated);
		assertEquals(1, updated.getSteps().size());
		assertEquals(kept, updated.getSteps().get(0).getName());
		assertFalse(stepExists(project, dropped), "the dropped step should be deleted");
	}

	@Test
	public void aDroppedStepAnotherScenarioUsesSurvives() throws Exception {
		Project project = createProject("Scenario-steps-shared");
		String shared = "Wash your hands " + System.nanoTime();
		Scenario first = createScenarioWithSteps(project, "Prepare lunch",
				List.of(newStep(project, shared)));
		Step sharedStep = getProjectRepository().findStepByProjectOrDomainAndName(project, shared);
		Scenario second = createScenarioWithSteps(project, "Prepare dinner",
				List.of(existingStep(project, sharedStep)));

		replaceSteps(project, first, new ArrayList<>());

		assertTrue(stepExists(project, shared), "a step another scenario still uses must survive");
		assertEquals(1, getProjectRepository().get(second).getSteps().size(),
				"the other scenario keeps the step");
	}

	@Test
	public void aDroppedSubScenarioSurvives() throws Exception {
		Project project = createProject("Scenario-steps-subscenario");
		Scenario sub = createScenarioWithSteps(project, "Log in " + System.nanoTime(),
				List.of(newStep(project, "Type the password " + System.nanoTime())));
		Scenario parent = createScenarioWithSteps(project, "Check out",
				List.of(existingStep(project, sub)));
		assertEquals(1, getProjectRepository().get(parent).getSteps().size(), "fixture");

		replaceSteps(project, parent, new ArrayList<>());

		Scenario reloadedSub = getProjectRepository().get(sub);
		assertNotNull(reloadedSub, "a dropped sub-scenario is a standalone scenario and stays");
		assertEquals(1, reloadedSub.getSteps().size(), "and it keeps its own steps");
	}
}
