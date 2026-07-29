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
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for goal management commands:
 * {@link EditGoalCommand} and {@link DeleteGoalCommand}.
 */
public class GoalCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("GoalTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	// -------------------------------------------------------------------------
	// EditGoalCommand
	// -------------------------------------------------------------------------

	@Test
	public void createGoal() throws Exception {
		Project project = createProject("Goal-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		// Project implements GoalContainer — passing it here sets both the parent
		// project and adds the goal to the project's goal list.
		cmd.setGoalContainer(project);
		cmd.setName("Easy to use");
		cmd.setText("The tool must be useable by both technical and non-technical stakeholders.");
		cmd = getCommandHandler().execute(cmd);

		Goal goal = cmd.getGoal();
		assertNotNull(goal, "goal should have been created");
		assertEquals("Easy to use", goal.getName(), "goal name should match");
		assertEquals("The tool must be useable by both technical and non-technical stakeholders.",
				goal.getText(), "goal text should match");
		assertDoesNotThrow(
				() -> getProjectRepository().findGoalByProjectOrDomainAndName(project, "Easy to use"),
				"newly created goal must be findable on the project");
	}

	@Test
	public void editGoal() throws Exception {
		Project project = createProject("Goal-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand createCmd = getProjectCommandFactory().newEditGoalCommand();
		createCmd.setEditedBy(admin);
		createCmd.setGoalContainer(project);
		createCmd.setName("Remote access");
		createCmd.setText("Users can access the system from remote sites.");
		createCmd = getCommandHandler().execute(createCmd);
		Goal original = createCmd.getGoal();

		EditGoalCommand editCmd = getProjectCommandFactory().newEditGoalCommand();
		editCmd.setEditedBy(admin);
		editCmd.setGoal(original);
		editCmd.setName("Remote access");
		editCmd.setText("Users can access the system from any location via a Web browser.");
		editCmd = getCommandHandler().execute(editCmd);

		Goal updated = editCmd.getGoal();
		assertEquals("Remote access", updated.getName(), "name should be unchanged");
		assertEquals("Users can access the system from any location via a Web browser.",
				updated.getText(), "text should have been updated");
	}

	@Test
	public void duplicateGoalNameIsRejected() throws Exception {
		Project project = createProject("Goal-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand first = getProjectCommandFactory().newEditGoalCommand();
		first.setEditedBy(admin);
		first.setGoalContainer(project);
		first.setName("Multi-user support");
		first.setText("First definition.");
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditGoalCommand dup = getProjectCommandFactory().newEditGoalCommand();
			dup.setEditedBy(admin);
			dup.setGoalContainer(project);
			dup.setName("Multi-user support");
			dup.setText("Duplicate definition.");
			getCommandHandler().execute(dup);
		}, "duplicate goal name on the same project should be rejected");
	}

	@Test
	public void editGoalWithMatchingVersionSucceeds() throws Exception {
		Project project = createProject("Goal-version-ok");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand createCmd = getProjectCommandFactory().newEditGoalCommand();
		createCmd.setEditedBy(admin);
		createCmd.setGoalContainer(project);
		createCmd.setName("Versioned goal");
		createCmd.setText("Initial text.");
		createCmd = getCommandHandler().execute(createCmd);
		Goal original = createCmd.getGoal();

		EditGoalCommand editCmd = getProjectCommandFactory().newEditGoalCommand();
		editCmd.setEditedBy(admin);
		editCmd.setGoal(original);
		editCmd.setName("Versioned goal");
		editCmd.setText("Updated with the current version.");
		editCmd.setExpectedVersion(original.getVersion());
		editCmd = getCommandHandler().execute(editCmd);

		assertEquals("Updated with the current version.", editCmd.getGoal().getText(),
				"matching-version update should be applied");
	}

	@Test
	public void editGoalWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("Goal-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand createCmd = getProjectCommandFactory().newEditGoalCommand();
		createCmd.setEditedBy(admin);
		createCmd.setGoalContainer(project);
		createCmd.setName("Contested goal");
		createCmd.setText("Initial text.");
		createCmd = getCommandHandler().execute(createCmd);
		Goal original = createCmd.getGoal();
		int staleVersion = original.getVersion();

		// First writer wins — this bumps the persisted version past staleVersion.
		EditGoalCommand firstEdit = getProjectCommandFactory().newEditGoalCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setGoal(original);
		firstEdit.setName("Contested goal");
		firstEdit.setText("First writer's change.");
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		// Second writer submits the now-stale version and must be rejected.
		assertThrows(EntityLockException.class, () -> {
			EditGoalCommand staleEdit = getProjectCommandFactory().newEditGoalCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setGoal(original);
			staleEdit.setName("Contested goal");
			staleEdit.setText("Second writer's stale change.");
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
	}

	@Test
	public void createGoalIgnoresExpectedVersion() throws Exception {
		Project project = createProject("Goal-version-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName("Create ignores version");
		cmd.setText("A create carries no goal id, so the version is not checked.");
		cmd.setExpectedVersion(99);
		cmd = getCommandHandler().execute(cmd);

		assertNotNull(cmd.getGoal(), "create should succeed regardless of expectedVersion");
	}

	// -------------------------------------------------------------------------
	// DeleteGoalCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteGoal() throws Exception {
		Project project = createProject("Goal-delete");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGoalCommand createCmd = getProjectCommandFactory().newEditGoalCommand();
		createCmd.setEditedBy(admin);
		createCmd.setGoalContainer(project);
		createCmd.setName("ToDelete");
		createCmd.setText("This goal will be deleted.");
		createCmd = getCommandHandler().execute(createCmd);
		Goal goal = createCmd.getGoal();

		DeleteGoalCommand deleteCmd = getProjectCommandFactory().newDeleteGoalCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setGoal(goal);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findGoalByProjectOrDomainAndName(project, "ToDelete"),
				"deleted goal should no longer be findable");
	}
}
