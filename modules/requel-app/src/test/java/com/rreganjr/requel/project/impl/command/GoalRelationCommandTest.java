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
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteGoalRelationCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.exception.GoalSelfRelationException;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for goal relation commands:
 * {@link EditGoalRelationCommand} and {@link DeleteGoalRelationCommand}.
 *
 * Goal relations link two distinct goals with a type of either Supports or
 * Conflicts. The command resolves goals by name at execute time, so only the
 * string names are needed — not the Goal objects themselves.
 */
public class GoalRelationCommandTest extends AbstractIntegrationTestCase {

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
		cmd.setOrganizationName("GoalRelationTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private Goal createGoal(Project project, String name) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText("Goal for relation tests.");
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGoal();
	}

	// -------------------------------------------------------------------------
	// EditGoalRelationCommand
	// -------------------------------------------------------------------------

	@Test
	public void createSupportingRelation() throws Exception {
		Project project = createProject("GoalRelation-supports");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Easy to use");
		createGoal(project, "Remote access");

		EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		// Goals are looked up by name at execute time
		cmd.setFromGoal("Remote access");
		cmd.setToGoal("Easy to use");
		cmd.setRelationType(GoalRelationType.Supports.name());
		cmd = getCommandHandler().execute(cmd);

		GoalRelation relation = cmd.getGoalRelation();
		assertNotNull(relation, "goal relation should have been created");
		assertEquals(GoalRelationType.Supports, relation.getRelationType(),
				"relation type should be Supports");
		assertEquals("Remote access", relation.getFromGoal().getName(),
				"from goal should be 'Remote access'");
		assertEquals("Easy to use", relation.getToGoal().getName(),
				"to goal should be 'Easy to use'");
	}

	@Test
	public void createConflictingRelation() throws Exception {
		Project project = createProject("GoalRelation-conflicts");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Easy to use");
		createGoal(project, "Don't impose a process");

		EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setFromGoal("Easy to use");
		cmd.setToGoal("Don't impose a process");
		cmd.setRelationType(GoalRelationType.Conflicts.name());
		cmd = getCommandHandler().execute(cmd);

		GoalRelation relation = cmd.getGoalRelation();
		assertNotNull(relation, "goal relation should have been created");
		assertEquals(GoalRelationType.Conflicts, relation.getRelationType(),
				"relation type should be Conflicts");
	}

	@Test
	public void editGoalRelation() throws Exception {
		Project project = createProject("GoalRelation-edit");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Multi-user support");
		createGoal(project, "Remote access");

		EditGoalRelationCommand createCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setFromGoal("Multi-user support");
		createCmd.setToGoal("Remote access");
		createCmd.setRelationType(GoalRelationType.Supports.name());
		createCmd = getCommandHandler().execute(createCmd);
		GoalRelation original = createCmd.getGoalRelation();

		EditGoalRelationCommand editCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setGoalRelation(original);
		editCmd.setFromGoal("Multi-user support");
		editCmd.setToGoal("Remote access");
		editCmd.setRelationType(GoalRelationType.Conflicts.name());
		editCmd = getCommandHandler().execute(editCmd);

		GoalRelation updated = editCmd.getGoalRelation();
		assertEquals(GoalRelationType.Conflicts, updated.getRelationType(),
				"relation type should have been changed to Conflicts");
	}

	@Test
	public void selfRelationIsRejected() throws Exception {
		Project project = createProject("GoalRelation-self");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Accessibility");

		assertThrows(GoalSelfRelationException.class, () -> {
			EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
			cmd.setEditedBy(admin);
			cmd.setProjectOrDomain(project);
			cmd.setFromGoal("Accessibility");
			cmd.setToGoal("Accessibility");
			cmd.setRelationType(GoalRelationType.Supports.name());
			getCommandHandler().execute(cmd);
		}, "a goal cannot have a relation to itself");
	}

	// -------------------------------------------------------------------------
	// DeleteGoalRelationCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteGoalRelation() throws Exception {
		Project project = createProject("GoalRelation-delete");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Don't impose top-down gathering");
		createGoal(project, "Don't impose a process");

		EditGoalRelationCommand createCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setFromGoal("Don't impose top-down gathering");
		createCmd.setToGoal("Don't impose a process");
		createCmd.setRelationType(GoalRelationType.Supports.name());
		createCmd = getCommandHandler().execute(createCmd);
		GoalRelation relation = createCmd.getGoalRelation();

		DeleteGoalRelationCommand deleteCmd = getProjectCommandFactory().newDeleteGoalRelationCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setGoalRelation(relation);
		getCommandHandler().execute(deleteCmd);

		// Reload the from-goal and verify the relation is gone
		Goal reloadedFromGoal = getProjectRepository()
				.findGoalByProjectOrDomainAndName(project, "Don't impose top-down gathering");
		assertTrue(reloadedFromGoal.getRelationsFromThisGoal().isEmpty(),
				"deleted relation should no longer appear on the from goal");
	}
}
