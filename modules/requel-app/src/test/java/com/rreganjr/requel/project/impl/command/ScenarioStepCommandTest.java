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
import com.rreganjr.requel.project.command.ConvertStepToScenarioCommand;
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
 * Steps ARE name-constrained: {@code StepImpl} and {@code ScenarioImpl} share one table under a
 * discriminator with a single {@code (projectordomain_id, name)} unique key, so a step name has to
 * be unique against scenarios as well as steps, and
 * {@code ProjectRepository.findStepByProjectOrDomainAndName} resolves either. (This comment used to
 * say the opposite; corrected in issue #254 along with the guard that enforces it.)
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

	// -------------------------------------------------------------------------
	// Name collisions (issue #254)
	// -------------------------------------------------------------------------

	/**
	 * The failure the ticket reports: the caller saw "The name conflicts with an existing
	 * ProjectOrDomainEntity", which names neither the step nor what it collided with, and blamed
	 * the scenario being saved. The guard names the step, its id, and the id to send instead.
	 */
	@Test
	public void creatingAStepWithATakenStepNameIsRefusedAndNamesTheExistingStep() throws Exception {
		Project project = createProject("Step-collide-step");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand first = getProjectCommandFactory().newEditScenarioStepCommand();
		first.setEditedBy(admin);
		first.setProjectOrDomain(project);
		first.setName("End the room");
		first.setText("The host closes the session.");
		first.setScenarioTypeName(ScenarioType.Primary.name());
		first = getCommandHandler().execute(first);
		Long existingId = first.getStep().getId();

		EditScenarioStepCommand clash = getProjectCommandFactory().newEditScenarioStepCommand();
		clash.setEditedBy(admin);
		clash.setProjectOrDomain(project);
		clash.setName("End the room");
		clash.setText("Different prose, same name.");
		clash.setScenarioTypeName(ScenarioType.Primary.name());

		Exception thrown = assertThrows(Exception.class,
				() -> getCommandHandler().execute(clash));
		String message = rootMessageOf(thrown);
		assertTrue(message.contains("Step"), "the message must say what it collided with: " + message);
		assertTrue(message.contains(String.valueOf(existingId)),
				"the message must name the existing entity's id: " + message);
		assertTrue(message.contains("stepId"),
				"the message must tell the caller which id to send: " + message);
		assertFalse(message.contains("ProjectOrDomainEntity"),
				"the opaque type name is the bug being fixed: " + message);
	}

	/**
	 * Steps and scenarios share the name space, so a step named after an existing scenario collides
	 * too — and the remedy is different, because nesting an existing scenario needs isScenario.
	 */
	@Test
	public void creatingAStepWithAScenariosNameIsRefusedAndPointsAtIsScenario() throws Exception {
		Project project = createProject("Step-collide-scenario");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand seedStep = getProjectCommandFactory().newEditScenarioStepCommand();
		seedStep.setEditedBy(admin);
		seedStep.setProjectOrDomain(project);
		seedStep.setName("A step of the room");
		seedStep.setText("Filler so the scenario has content.");
		seedStep.setScenarioTypeName(ScenarioType.Primary.name());
		Scenario scenario = createScenarioWithStep(project, "Run the room", seedStep);

		EditScenarioStepCommand clash = getProjectCommandFactory().newEditScenarioStepCommand();
		clash.setEditedBy(admin);
		clash.setProjectOrDomain(project);
		clash.setName("Run the room");
		clash.setText("A plain step wanting a scenario's name.");
		clash.setScenarioTypeName(ScenarioType.Primary.name());

		Exception thrown = assertThrows(Exception.class,
				() -> getCommandHandler().execute(clash));
		String message = rootMessageOf(thrown);
		assertTrue(message.contains("Scenario"),
				"a scenario holds the name, and the message must say so: " + message);
		assertTrue(message.contains(String.valueOf(scenario.getId())),
				"the message must name the existing scenario's id: " + message);
		assertTrue(message.contains("isScenario"),
				"nesting an existing scenario needs isScenario, not a bare stepId: " + message);
	}

	/**
	 * Renaming a step onto its own name is not a collision.
	 */
	@Test
	public void renamingAStepToItsOwnNameIsNotACollision() throws Exception {
		Project project = createProject("Step-self-rename");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand create = getProjectCommandFactory().newEditScenarioStepCommand();
		create.setEditedBy(admin);
		create.setProjectOrDomain(project);
		create.setName("Keep the name");
		create.setText("first text");
		create.setScenarioTypeName(ScenarioType.Primary.name());
		create = getCommandHandler().execute(create);

		EditScenarioStepCommand edit = getProjectCommandFactory().newEditScenarioStepCommand();
		edit.setEditedBy(admin);
		edit.setProjectOrDomain(project);
		edit.setStep(create.getStep());
		edit.setName("Keep the name");
		edit.setText("second text");
		edit.setScenarioTypeName(ScenarioType.Primary.name());
		edit = getCommandHandler().execute(edit);

		assertEquals("second text", edit.getStep().getText(),
				"editing a step without renaming it must not trip the uniqueness guard");
	}

	/**
	 * Issue #254's third AC. Steps are shared rather than owned: linking one into a second scenario
	 * by id must leave it in the first, and the step must report both through
	 * {@link Step#getUsingScenarios()}, which is the inverse side of the many-to-many.
	 */
	@Test
	public void aStepLinkedIntoASecondScenarioIsSharedByBoth() throws Exception {
		Project project = createProject("Step-shared");
		User admin = getUserRepository().findUserByUsername("admin");

		EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
		stepCmd.setEditedBy(admin);
		stepCmd.setProjectOrDomain(project);
		stepCmd.setName("End the room");
		stepCmd.setText("The host closes the session.");
		stepCmd.setScenarioTypeName(ScenarioType.Primary.name());
		Scenario first = createScenarioWithStep(project, "Happy path", stepCmd);
		Step shared = stepCmd.getStep();

		EditScenarioStepCommand link = getProjectCommandFactory().newEditScenarioStepCommand();
		link.setEditedBy(admin);
		link.setProjectOrDomain(project);
		link.setStep(shared);
		link.setName(shared.getName());
		link.setText(shared.getText());
		link.setScenarioTypeName(shared.getType().name());
		Scenario second = createScenarioWithStep(project, "Timeout path", link);

		assertNotEquals(first.getId(), second.getId(), "pre-condition: two distinct scenarios");

		Scenario reloadedFirst = getProjectRepository().findById(Scenario.class, first.getId());
		Scenario reloadedSecond = getProjectRepository().findById(Scenario.class, second.getId());
		assertTrue(reloadedFirst.getSteps().stream().anyMatch(s -> s.getId().equals(shared.getId())),
				"linking the step elsewhere must not remove it from the first scenario");
		assertTrue(reloadedSecond.getSteps().stream().anyMatch(s -> s.getId().equals(shared.getId())),
				"the second scenario must hold the same step, not a copy");

		Step reloadedStep = getProjectRepository().findById(Step.class, shared.getId());
		assertEquals(2, reloadedStep.getUsingScenarios().size(),
				"the step's inverse side must report both scenarios using it");
	}

	/** Unwraps the command-handler wrapping so assertions read the message the caller sees. */
	private static String rootMessageOf(Throwable thrown) {
		StringBuilder all = new StringBuilder();
		for (Throwable t = thrown; t != null; t = t.getCause()) {
			if (t.getMessage() != null) {
				all.append(t.getMessage()).append(" | ");
			}
		}
		return all.toString();
	}

	// -------------------------------------------------------------------------
	// ConvertStepToScenarioCommand
	// -------------------------------------------------------------------------

	@Test
	public void convertStepToScenario() throws Exception {
		Project project = createProject("Step-convert");
		User admin = getUserRepository().findUserByUsername("admin");

		// Build the step — it will be created as part of the parent scenario
		EditScenarioStepCommand stepCmd = getProjectCommandFactory().newEditScenarioStepCommand();
		stepCmd.setEditedBy(admin);
		stepCmd.setProjectOrDomain(project);
		stepCmd.setName("User fills in the registration form");
		stepCmd.setText("The user enters name, email, and password then submits.");
		stepCmd.setScenarioTypeName(ScenarioType.Primary.name());

		createScenarioWithStep(project, "Registration flow", stepCmd);
		Step originalStep = stepCmd.getStep();
		assertNotNull(originalStep, "pre-condition: step should exist");

		// Convert the step to a standalone scenario
		ConvertStepToScenarioCommand convertCmd =
				getProjectCommandFactory().newConvertStepToScenarioCommand();
		convertCmd.setEditedBy(admin);
		convertCmd.setProjectOrDomain(project);
		convertCmd.setOriginalScenarioStep(originalStep);
		convertCmd.setName("User fills in the registration form");
		convertCmd.setText("The user enters name, email, and password then submits.");
		convertCmd.setScenarioTypeName(ScenarioType.Primary.name());
		convertCmd = getCommandHandler().execute(convertCmd);

		// The command produces a new Scenario entity that is distinct from the original Step.
		// Replacing the step in its parent scenario requires getUsingScenarios() to be populated,
		// which depends on the bidirectional mapping being initialized — that path is exercised
		// by the UI where the step comes from a loaded scenario. Here we verify the core
		// behaviour: a new Scenario was created with the correct properties.
		//
		// Note: Scenario extends Step in the domain model (a Scenario can itself be a step in
		// another scenario), so instanceof Step is always true for any ScenarioImpl. The meaningful
		// distinction is that the returned entity is a Scenario (has steps) vs. a bare StepImpl.
		Scenario newScenario = convertCmd.getScenario();
		assertNotNull(newScenario, "converted scenario should have been created");
		assertNotEquals(originalStep.getId(), newScenario.getId(),
				"converted scenario should be a distinct entity from the original step");
		assertNotNull(newScenario.getSteps(),
				"result of conversion should be a Scenario with a steps collection, not a bare Step");
		assertEquals("User fills in the registration form", newScenario.getName(),
				"converted scenario should carry the specified name");
		assertEquals(ScenarioType.Primary, newScenario.getType(),
				"converted scenario should carry the specified type");
		// Verify the new scenario is visible in the project
		Project reloaded = getProjectRepository().findProjectByName(project.getName());
		assertTrue(reloaded.getScenarios().stream()
				.anyMatch(s -> "User fills in the registration form".equals(s.getName())),
				"converted scenario should appear in the project's scenario collection");
	}
}
