/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.RepairProjectStakeholdersCommand;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.command.EditUserCommand;

/**
 * Integration tests for the RepairProjectStakeholders maintenance command (issue #256).
 *
 * <p>
 * The command exists for rows left behind by older project-creating paths, so every test
 * here manufactures that state the only way the current code allows: create a project
 * normally, then sever the creator's stakeholder association.
 *
 * @author ron
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepairProjectStakeholdersIT extends AbstractIntegrationTestCase {

	private static final String PROJECT_EDIT_KEY =
			StakeholderPermissionImpl.generatePermissionKey(Project.class,
					StakeholderPermissionType.Edit);

	@BeforeAll
	void setUp() throws Exception {
		initializeBaselineData();
	}

	@Test
	void restoresTheCreatorsRowWithTheFullPermissionSet() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		String projectName = "repair-restore-" + System.currentTimeMillis();
		Project project = createProject(admin, projectName);
		int available = getProjectRepository().findAvailableStakeholderPermissions().size();

		removeStakeholder(admin, project, admin);
		assertNull(findStakeholder(projectName, admin),
				"precondition: the creator must hold no stakeholder row");

		RepairProjectStakeholdersCommand cmd = repair(admin, projectName);

		UserStakeholder restored = findStakeholder(projectName, admin);
		assertNotNull(restored, "the creator's stakeholder row must be restored");
		assertEquals(available, restored.getStakeholderPermissions().size(),
				"a repaired creator must end up with exactly what creation grants");
		assertEquals(1, cmd.getProjectsScanned());
		assertEquals(1, cmd.getStakeholdersCreated());
		assertEquals(available, cmd.getPermissionsGranted());
		assertEquals(0, cmd.getProjectsSkipped());
	}

	@Test
	void isIdempotent() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		String projectName = "repair-idempotent-" + System.currentTimeMillis();
		Project project = createProject(admin, projectName);
		removeStakeholder(admin, project, admin);

		repair(admin, projectName);
		RepairProjectStakeholdersCommand second = repair(admin, projectName);

		assertEquals(1, second.getProjectsScanned());
		assertEquals(0, second.getStakeholdersCreated(),
				"a second run must create nothing");
		assertEquals(0, second.getPermissionsGranted(),
				"a second run must grant nothing");
	}

	@Test
	void fillsPermissionGapsWithoutCreatingASecondRow() throws Exception {
		// The other shape of the damage: the row survived but the permissions did not.
		// Reduced through EditUserStakeholder (which revokes anything absent from the set)
		// rather than by clearing the collection by hand, so the change actually persists.
		User admin = getUserRepository().findUserByUsername("admin");
		String projectName = "repair-partial-" + System.currentTimeMillis();
		Project project = createProject(admin, projectName);
		int available = getProjectRepository().findAvailableStakeholderPermissions().size();

		setStakeholderPermissions(admin, project, "admin", Set.of(PROJECT_EDIT_KEY));
		assertEquals(1, findStakeholder(projectName, admin).getStakeholderPermissions().size(),
				"precondition: the creator must be down to a single permission");

		RepairProjectStakeholdersCommand cmd = repair(admin, projectName);

		assertEquals(0, cmd.getStakeholdersCreated(), "the existing row must be reused");
		assertEquals(available - 1, cmd.getPermissionsGranted(),
				"only the missing permissions are granted");
		assertEquals(available, findStakeholder(projectName, admin)
				.getStakeholderPermissions().size());
	}

	@Test
	void repairsEveryProjectWhenNoNameIsGiven() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		String first = "repair-all-a-" + ts;
		String second = "repair-all-b-" + ts;
		removeStakeholder(admin, createProject(admin, first), admin);
		removeStakeholder(admin, createProject(admin, second), admin);

		RepairProjectStakeholdersCommand cmd = repair(admin, null);

		assertTrue(cmd.getProjectsScanned() >= 2,
				"a nameless run must scan the whole estate, not one project");
		assertNotNull(findStakeholder(first, admin));
		assertNotNull(findStakeholder(second, admin));
	}

	@Test
	void requiresTheSystemAdministratorRole() throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		long ts = System.currentTimeMillis();
		String projectName = "repair-auth-" + ts;
		createProject(admin, projectName);

		String username = "repair-user-" + ts;
		createUser(admin, username);
		User ordinary = getUserRepository().findUserByUsername(username);

		assertThrows(AuthorizationException.class, () -> repair(ordinary, projectName));
	}

	// ---- helpers -------------------------------------------------------------

	private RepairProjectStakeholdersCommand repair(User actor, String projectName)
			throws Exception {
		RepairProjectStakeholdersCommand cmd = getProjectCommandFactory()
				.newRepairProjectStakeholdersCommand();
		cmd.setEditedBy(actor);
		cmd.setProjectName(projectName);
		return getCommandHandler().execute(cmd);
	}

	/**
	 * The repository's finder throws rather than returning null when there is no row, and
	 * "no row" is precisely the state these tests set up, so absence is translated here.
	 */
	private UserStakeholder findStakeholder(String projectName, User user) {
		Project project = getProjectRepository().findProjectByName(projectName);
		try {
			return getProjectRepository().findStakeholderByProjectOrDomainAndUser(project, user);
		} catch (NoSuchEntityException e) {
			return null;
		}
	}

	private Project createProject(User owner, String name) throws Exception {
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(owner);
		cmd.setName(name);
		cmd.setText("repair-project-stakeholders integration test");
		cmd.setOrganizationName("RepairTestOrg-" + name);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private void removeStakeholder(User actor, Project project, User member) throws Exception {
		UserStakeholder stakeholder = getProjectRepository()
				.findStakeholderByProjectOrDomainAndUser(getProjectRepository().get(project), member);
		DeleteStakeholderCommand cmd = getProjectCommandFactory().newDeleteStakeholderCommand();
		cmd.setEditedBy(actor);
		cmd.setStakeholder(stakeholder);
		getCommandHandler().execute(cmd);
	}

	private void createUser(User actor, String username) throws Exception {
		EditUserCommand cmd = getUserCommandFactory().newEditUserCommand();
		cmd.setEditedBy(actor);
		cmd.setUsername(username);
		cmd.setPassword("test-pass");
		cmd.setRepassword("test-pass");
		cmd.setName(username);
		cmd.setEmailAddress(username + "@example.com");
		cmd.setPhoneNumber("");
		cmd.setOrganizationName("RepairTestOrg");
		// A user with no role at all fails bean validation on the way in.
		cmd.addUserRoleName("ProjectUserRole");
		getCommandHandler().execute(cmd);
	}

	/**
	 * Replace a stakeholder's permission set. EditUserStakeholder revokes anything absent
	 * from the supplied keys, which is what makes it usable to manufacture a gap.
	 */
	private void setStakeholderPermissions(User actor, Project project, String username,
			Set<String> permissionKeys) throws Exception {
		Project managed = getProjectRepository().get(project);
		UserStakeholder existing = getProjectRepository()
				.findStakeholderByProjectOrDomainAndUser(managed,
						getUserRepository().findUserByUsername(username));

		EditUserStakeholderCommand cmd = getProjectCommandFactory()
				.newEditUserStakeholderCommand();
		cmd.setEditedBy(actor);
		cmd.setProjectOrDomain(managed);
		cmd.setUsername(username);
		cmd.setStakeholderPermissions(permissionKeys);
		// Without the existing row the command takes its create path and rejects the
		// username as already a stakeholder (the registrar resolves it the same way).
		cmd.setStakeholder(existing);
		cmd.setExpectedVersion(existing.getVersion());
		getCommandHandler().execute(cmd);
	}

}
