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

import java.util.Collections;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for project stakeholder management commands:
 * {@link EditUserStakeholderCommand}, {@link EditNonUserStakeholderCommand},
 * and {@link DeleteStakeholderCommand}.
 */
public class StakeholderCommandTest extends AbstractIntegrationTestCase {

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Creates a uniquely-named project owned by admin and returns it.
	 * Admin already has ProjectUserRole ensured by AbstractIntegrationTestCase.
	 */
	private Project createProject(String label) throws Exception {
		long ts = System.currentTimeMillis();
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("StakeholderTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	// -------------------------------------------------------------------------
	// EditUserStakeholderCommand
	// -------------------------------------------------------------------------

	@Test
	public void createUserStakeholder() throws Exception {
		Project project = createProject("UserStakeholder-create");
		User admin = getUserRepository().findUserByUsername("admin");
		// "project" user already has ProjectUserRole (granted in AbstractIntegrationTestCase)
		User projectUser = getUserRepository().findUserByUsername("project");

		EditUserStakeholderCommand cmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setUsername(projectUser.getUsername());
		cmd.setStakeholderPermissions(Collections.emptySet());
		cmd = getCommandHandler().execute(cmd);

		UserStakeholder stakeholder = cmd.getStakeholder();
		assertNotNull(stakeholder, "stakeholder should have been created");
		assertEquals(projectUser, stakeholder.getUser(), "stakeholder should wrap the project user");
		// Verify via repository (reloads from DB) — the in-memory `project` is stale after the command
		assertDoesNotThrow(
				() -> getProjectRepository().findStakeholderByProjectOrDomainAndUser(project, projectUser),
				"newly created user stakeholder must be findable on the project");
	}

	@Test
	public void editUserStakeholder() throws Exception {
		Project project = createProject("UserStakeholder-edit");
		User admin = getUserRepository().findUserByUsername("admin");
		User projectUser = getUserRepository().findUserByUsername("project");

		// Create the stakeholder first
		EditUserStakeholderCommand createCmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setUsername(projectUser.getUsername());
		createCmd.setStakeholderPermissions(Collections.emptySet());
		createCmd = getCommandHandler().execute(createCmd);
		UserStakeholder original = createCmd.getStakeholder();

		// Edit: assign to a team
		EditUserStakeholderCommand editCmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setStakeholder(original);
		editCmd.setUsername(projectUser.getUsername());
		editCmd.setStakeholderPermissions(Collections.emptySet());
		editCmd.setTeamName("Dev");
		editCmd = getCommandHandler().execute(editCmd);

		UserStakeholder updated = editCmd.getStakeholder();
		assertNotNull(updated.getTeam(), "stakeholder should now belong to a team");
		assertEquals("Dev", updated.getTeam().getName(), "team name should match what was set");
	}

	@Test
	public void duplicateUserStakeholderIsRejected() throws Exception {
		Project project = createProject("UserStakeholder-dup");
		User admin = getUserRepository().findUserByUsername("admin");
		User projectUser = getUserRepository().findUserByUsername("project");

		// Create once
		EditUserStakeholderCommand first = getProjectCommandFactory().newEditUserStakeholderCommand();
		first.setEditedBy(admin);
		first.setProjectOrDomain(project);
		first.setUsername(projectUser.getUsername());
		first.setStakeholderPermissions(Collections.emptySet());
		getCommandHandler().execute(first);

		// Creating a second stakeholder for the same user on the same project should fail
		assertThrows(EntityException.class, () -> {
			EditUserStakeholderCommand dup = getProjectCommandFactory().newEditUserStakeholderCommand();
			dup.setEditedBy(admin);
			dup.setProjectOrDomain(project);
			dup.setUsername(projectUser.getUsername());
			dup.setStakeholderPermissions(Collections.emptySet());
			getCommandHandler().execute(dup);
		}, "duplicate user stakeholder should be rejected");
	}

	// -------------------------------------------------------------------------
	// EditNonUserStakeholderCommand
	// -------------------------------------------------------------------------

	@Test
	public void createNonUserStakeholder() throws Exception {
		Project project = createProject("NonUserStakeholder-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand cmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("FASB");
		cmd.setText("Financial Accounting Standards Board");
		cmd = getCommandHandler().execute(cmd);

		NonUserStakeholder stakeholder = cmd.getStakeholder();
		assertNotNull(stakeholder, "stakeholder should have been created");
		assertEquals("FASB", stakeholder.getName(), "stakeholder name should match");
		assertEquals("Financial Accounting Standards Board", stakeholder.getText(),
				"stakeholder description should match");
		assertFalse(stakeholder.isUserStakeholder(), "authority stakeholder must not be a user stakeholder");
		// Verify via repository (reloads from DB) — the in-memory `project` is stale after the command
		assertDoesNotThrow(
				() -> getProjectRepository().findStakeholderByProjectOrDomainAndName(project, "FASB"),
				"newly created non-user stakeholder must be findable on the project");
	}

	@Test
	public void editNonUserStakeholder() throws Exception {
		Project project = createProject("NonUserStakeholder-edit");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand createCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("ISO");
		createCmd.setText("International Organization for Standardization");
		createCmd = getCommandHandler().execute(createCmd);
		NonUserStakeholder original = createCmd.getStakeholder();

		EditNonUserStakeholderCommand editCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setStakeholder(original);
		editCmd.setName("ISO");
		editCmd.setText("Updated ISO description");
		editCmd = getCommandHandler().execute(editCmd);

		NonUserStakeholder updated = editCmd.getStakeholder();
		assertEquals("Updated ISO description", updated.getText(), "description should have been updated");
		assertEquals("ISO", updated.getName(), "name should be unchanged");
	}

	@Test
	public void editNonUserStakeholderWithMatchingVersionSucceeds() throws Exception {
		Project project = createProject("NonUserStakeholder-version-ok");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand createCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Versioned authority");
		createCmd.setText("Initial description.");
		createCmd = getCommandHandler().execute(createCmd);
		NonUserStakeholder original = createCmd.getStakeholder();

		EditNonUserStakeholderCommand editCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setStakeholder(original);
		editCmd.setName("Versioned authority");
		editCmd.setText("Updated with the current version.");
		editCmd.setExpectedVersion(original.getVersion());
		editCmd = getCommandHandler().execute(editCmd);

		assertEquals("Updated with the current version.", editCmd.getStakeholder().getText(),
				"matching-version update should be applied");
	}

	@Test
	public void editNonUserStakeholderWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("NonUserStakeholder-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand createCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("Contested authority");
		createCmd.setText("Initial description.");
		createCmd = getCommandHandler().execute(createCmd);
		NonUserStakeholder original = createCmd.getStakeholder();
		int staleVersion = original.getVersion();

		EditNonUserStakeholderCommand firstEdit = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setProjectOrDomain(project);
		firstEdit.setStakeholder(original);
		firstEdit.setName("Contested authority");
		firstEdit.setText("First writer's change.");
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditNonUserStakeholderCommand staleEdit = getProjectCommandFactory().newEditNonUserStakeholderCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setProjectOrDomain(project);
			staleEdit.setStakeholder(original);
			staleEdit.setName("Contested authority");
			staleEdit.setText("Second writer's stale change.");
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
	}

	@Test
	public void editUserStakeholderWithStaleVersionIsRejected() throws Exception {
		Project project = createProject("UserStakeholder-version-stale");
		User admin = getUserRepository().findUserByUsername("admin");
		User projectUser = getUserRepository().findUserByUsername("project");

		EditUserStakeholderCommand createCmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setUsername(projectUser.getUsername());
		createCmd.setStakeholderPermissions(Collections.emptySet());
		createCmd = getCommandHandler().execute(createCmd);
		UserStakeholder original = createCmd.getStakeholder();
		int staleVersion = original.getVersion();

		EditUserStakeholderCommand firstEdit = getProjectCommandFactory().newEditUserStakeholderCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setProjectOrDomain(project);
		firstEdit.setStakeholder(original);
		firstEdit.setUsername(projectUser.getUsername());
		firstEdit.setStakeholderPermissions(Collections.emptySet());
		firstEdit.setTeamName("Dev");
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditUserStakeholderCommand staleEdit = getProjectCommandFactory().newEditUserStakeholderCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setProjectOrDomain(project);
			staleEdit.setStakeholder(original);
			staleEdit.setUsername(projectUser.getUsername());
			staleEdit.setStakeholderPermissions(Collections.emptySet());
			staleEdit.setTeamName("QA");
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
	}

	@Test
	public void duplicateNonUserStakeholderIsRejected() throws Exception {
		Project project = createProject("NonUserStakeholder-dup");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand first = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		first.setEditedBy(admin);
		first.setProjectOrDomain(project);
		first.setName("DupAuthority");
		first.setText("first");
		getCommandHandler().execute(first);

		assertThrows(EntityException.class, () -> {
			EditNonUserStakeholderCommand dup = getProjectCommandFactory().newEditNonUserStakeholderCommand();
			dup.setEditedBy(admin);
			dup.setProjectOrDomain(project);
			dup.setName("DupAuthority");
			dup.setText("second");
			getCommandHandler().execute(dup);
		}, "duplicate non-user stakeholder name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteStakeholderCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteUserStakeholder() throws Exception {
		Project project = createProject("DeleteUserStakeholder");
		User admin = getUserRepository().findUserByUsername("admin");
		User projectUser = getUserRepository().findUserByUsername("project");

		EditUserStakeholderCommand createCmd = getProjectCommandFactory().newEditUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setUsername(projectUser.getUsername());
		createCmd.setStakeholderPermissions(Collections.emptySet());
		createCmd = getCommandHandler().execute(createCmd);
		Stakeholder stakeholder = createCmd.getStakeholder();

		DeleteStakeholderCommand deleteCmd = getProjectCommandFactory().newDeleteStakeholderCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setStakeholder(stakeholder);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findStakeholderByProjectOrDomainAndUser(project, projectUser),
				"deleted user stakeholder should no longer be findable");
	}

	@Test
	public void deleteNonUserStakeholder() throws Exception {
		Project project = createProject("DeleteNonUserStakeholder");
		User admin = getUserRepository().findUserByUsername("admin");

		EditNonUserStakeholderCommand createCmd = getProjectCommandFactory().newEditNonUserStakeholderCommand();
		createCmd.setEditedBy(admin);
		createCmd.setProjectOrDomain(project);
		createCmd.setName("ToDelete");
		createCmd.setText("this stakeholder will be deleted");
		createCmd = getCommandHandler().execute(createCmd);
		Stakeholder stakeholder = createCmd.getStakeholder();

		DeleteStakeholderCommand deleteCmd = getProjectCommandFactory().newDeleteStakeholderCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setStakeholder(stakeholder);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findStakeholderByProjectOrDomainAndName(project, "ToDelete"),
				"deleted non-user stakeholder should no longer be findable");
	}
}
