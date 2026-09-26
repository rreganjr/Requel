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
import com.rreganjr.platform.exception.EntityLockException;
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
import com.rreganjr.validator.EntityValidationException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for goal relation commands:
 * {@link EditGoalRelationCommand} and {@link DeleteGoalRelationCommand}.
 *
 * Goal relations link two distinct goals with a {@link GoalRelationType}. The
 * command resolves goals by name at execute time, so only the string names are
 * needed — not the Goal objects themselves. Issue #257 added five types, the
 * permitted-values message, and the one-per-direction and symmetric-reverse checks.
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

	/** Create (relation null) or edit a relation by goal names. */
	private GoalRelation relate(Project project, GoalRelation relation, String from, String to,
			String type) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalRelationCommand cmd = getProjectCommandFactory().newEditGoalRelationCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setGoalRelation(relation);
		cmd.setFromGoal(from);
		cmd.setToGoal(to);
		cmd.setRelationType(type);
		return getCommandHandler().execute(cmd).getGoalRelation();
	}

	private EntityValidationException refused(Project project, GoalRelation relation, String from,
			String to, String type) {
		return assertThrows(EntityValidationException.class,
				() -> relate(project, relation, from, to, type));
	}

	private static void assertRefusedOn(EntityValidationException e, String property) {
		assertTrue(Arrays.asList(e.getEntityPropertyNames()).contains(property),
				"refused on " + property + ": " + Arrays.toString(e.getEntityPropertyNames()));
	}

	// -------------------------------------------------------------------------
	// EditGoalRelationCommand
	// -------------------------------------------------------------------------

	@Test
	public void everyRelationTypeIsAcceptedAndReadBack() throws Exception {
		Project project = createProject("GoalRelation-all-types");
		createGoal(project, "Hub");
		for (GoalRelationType type : GoalRelationType.values()) {
			createGoal(project, "Spoke " + type.name());
			GoalRelation relation = relate(project, null, "Hub", "Spoke " + type.name(), type.name());
			assertEquals(type, relation.getRelationType(), type.name());
		}
		Goal hub = getProjectRepository().findGoalByProjectOrDomainAndName(project, "Hub");
		assertEquals(GoalRelationType.values().length, hub.getRelationsFromThisGoal().size());
	}

	@Test
	public void relationTypeIsCaseInsensitive() throws Exception {
		Project project = createProject("GoalRelation-case");
		createGoal(project, "Lower from");
		createGoal(project, "Lower to");
		GoalRelation relation = relate(project, null, "Lower from", "Lower to", " dependson ");
		assertEquals(GoalRelationType.DependsOn, relation.getRelationType());
	}

	@Test
	public void unknownRelationTypeNamesThePermittedValues() throws Exception {
		Project project = createProject("GoalRelation-unknown");
		createGoal(project, "Unknown from");
		createGoal(project, "Unknown to");
		EntityValidationException e = refused(project, null, "Unknown from", "Unknown to", "Blocks");
		assertRefusedOn(e, "relationType");
		assertTrue(e.getMessage().contains("Blocks"), e.getMessage());
		assertTrue(e.getMessage().contains(GoalRelationType.permittedValues()), e.getMessage());
	}

	@Test
	public void secondRelationInTheSameDirectionIsRefused() throws Exception {
		Project project = createProject("GoalRelation-repeat");
		createGoal(project, "Repeat from");
		createGoal(project, "Repeat to");
		relate(project, null, "Repeat from", "Repeat to", "Supports");

		EntityValidationException e = refused(project, null, "Repeat from", "Repeat to", "Measures");
		assertRefusedOn(e, "toGoal");
		assertTrue(e.getMessage().contains("already has a Supports relation"), e.getMessage());
		Goal from = getProjectRepository().findGoalByProjectOrDomainAndName(project, "Repeat from");
		assertEquals(1, from.getRelationsFromThisGoal().size(), "no second row");
	}

	@Test
	public void reverseOfASymmetricRelationWithTheSameTypeIsRefused() throws Exception {
		Project project = createProject("GoalRelation-symmetric");
		createGoal(project, "Sym A");
		createGoal(project, "Sym B");
		relate(project, null, "Sym A", "Sym B", "Conflicts");

		EntityValidationException e = refused(project, null, "Sym B", "Sym A", "Conflicts");
		assertRefusedOn(e, "toGoal");
		assertTrue(e.getMessage().contains("holds in both directions"), e.getMessage());
	}

	@Test
	public void reverseWithADifferentTypeIsAllowed() throws Exception {
		Project project = createProject("GoalRelation-reverse-other");
		createGoal(project, "Rev A");
		createGoal(project, "Rev B");
		createGoal(project, "Rev C");
		relate(project, null, "Rev A", "Rev B", "Conflicts");
		relate(project, null, "Rev A", "Rev C", "Conflicts");

		assertEquals(GoalRelationType.Supports,
				relate(project, null, "Rev B", "Rev A", "Supports").getRelationType());
		assertEquals(GoalRelationType.Duplicates,
				relate(project, null, "Rev C", "Rev A", "Duplicates").getRelationType());
	}

	@Test
	public void changingATypeToASymmetricOneWhoseReverseExistsIsRefused() throws Exception {
		Project project = createProject("GoalRelation-edit-symmetric");
		createGoal(project, "Edit A");
		createGoal(project, "Edit B");
		GoalRelation forward = relate(project, null, "Edit A", "Edit B", "Supports");
		relate(project, null, "Edit B", "Edit A", "Conflicts");

		EntityValidationException e = refused(project, forward, "Edit A", "Edit B", "Conflicts");
		assertRefusedOn(e, "toGoal");
	}

	@Test
	public void editingARelationDoesNotCollideWithItself() throws Exception {
		Project project = createProject("GoalRelation-self-collide");
		createGoal(project, "Self A");
		createGoal(project, "Self B");
		GoalRelation relation = relate(project, null, "Self A", "Self B", "Conflicts");

		assertEquals(GoalRelationType.Conflicts,
				relate(project, relation, "Self A", "Self B", "Conflicts").getRelationType());
		assertEquals(GoalRelationType.Duplicates,
				relate(project, relation, "Self A", "Self B", "Duplicates").getRelationType());
	}

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
	public void editGoalRelationWithMatchingVersionSucceeds() throws Exception {
		Project project = createProject("GoalRelation-version-ok");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Version from goal");
		createGoal(project, "Version to goal");

		EditGoalRelationCommand createCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setFromGoal("Version from goal");
		createCmd.setToGoal("Version to goal");
		createCmd.setRelationType(GoalRelationType.Supports.name());
		createCmd = getCommandHandler().execute(createCmd);
		GoalRelation original = createCmd.getGoalRelation();

		EditGoalRelationCommand editCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setGoalRelation(original);
		editCmd.setFromGoal("Version from goal");
		editCmd.setToGoal("Version to goal");
		editCmd.setRelationType(GoalRelationType.Conflicts.name());
		editCmd.setExpectedVersion(original.getVersion());
		editCmd = getCommandHandler().execute(editCmd);

		assertEquals(GoalRelationType.Conflicts, editCmd.getGoalRelation().getRelationType(),
				"matching-version update should be applied");
	}

	@Test
	public void editGoalRelationWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("GoalRelation-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");
		createGoal(project, "Contested from goal");
		createGoal(project, "Contested to goal");

		EditGoalRelationCommand createCmd = getProjectCommandFactory().newEditGoalRelationCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setFromGoal("Contested from goal");
		createCmd.setToGoal("Contested to goal");
		createCmd.setRelationType(GoalRelationType.Supports.name());
		createCmd = getCommandHandler().execute(createCmd);
		GoalRelation original = createCmd.getGoalRelation();
		int staleVersion = original.getVersion();

		EditGoalRelationCommand firstEdit = getProjectCommandFactory().newEditGoalRelationCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setProjectOrDomain(project);
		firstEdit.setGoalRelation(original);
		firstEdit.setFromGoal("Contested from goal");
		firstEdit.setToGoal("Contested to goal");
		firstEdit.setRelationType(GoalRelationType.Conflicts.name());
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditGoalRelationCommand staleEdit = getProjectCommandFactory().newEditGoalRelationCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setProjectOrDomain(project);
			staleEdit.setGoalRelation(original);
			staleEdit.setFromGoal("Contested from goal");
			staleEdit.setToGoal("Contested to goal");
			staleEdit.setRelationType(GoalRelationType.Supports.name());
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
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
