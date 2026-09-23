/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.HashSet;
import java.util.Set;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class EditProjectCommandImplTest extends AbstractIntegrationTestCase {

	private User ensureProjectCapableAdmin() throws Exception {
		User creator = getUserRepository().findUserByUsername("admin");
		if (!creator.hasRole(ProjectUserRole.class)) {
			var editUser = getUserCommandFactory().newEditUserCommand();
			editUser.setEditedBy(creator);
			editUser.setUser(creator);
			editUser.setUsername(creator.getUsername());
			editUser.setName(creator.getName());
			editUser.setEmailAddress(creator.getEmailAddress());
			editUser.setPhoneNumber(creator.getPhoneNumber());
			editUser.setOrganizationName(creator.getOrganization().getName());
			editUser.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
			getCommandHandler().execute(editUser);
			creator = getUserRepository().findUserByUsername("admin");
		}
		return creator;
	}

	private Project createProject(String label) throws Exception {
		long uniqueifier = System.currentTimeMillis();
		User creator = ensureProjectCapableAdmin();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(creator);
		command.setName(label + " " + uniqueifier);
		command.setText("Description " + uniqueifier);
		command.setOrganizationName("Org " + uniqueifier);
		command = getCommandHandler().execute(command);
		return command.getProject();
	}

	@Test
	public void testProjectCreation() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		String projectDescription = "This is a test project " + uniqueifier;
		String organizationName = "Text Organization " + uniqueifier;
		User creator = ensureProjectCapableAdmin();
		Set<Stakeholder> expectedStakeholders = new HashSet<Stakeholder>();
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(creator);
		command.setName(projectName);
		command.setText(projectDescription);
		command.setOrganizationName(organizationName);
		command = getCommandHandler().execute(command);

		Project project = command.getProject();
		for (Stakeholder stakeholder : project.getStakeholders()) {
			if (stakeholder.isUserStakeholder()
					&& creator.equals(((UserStakeholder) stakeholder).getUser())) {
				expectedStakeholders.add(stakeholder);
			}
		}
		assertEquals(creator, project.getCreatedBy());
		assertEquals(projectName, project.getName());
		assertEquals("Project: " + projectName, project.getDescription());
		assertEquals(organizationName, project.getOrganization().getName());
		assertTrue(project.getStakeholders().containsAll(expectedStakeholders));
	}

	@Test
	public void editExistingProjectUpdatesNameTextAndOrganization() throws Exception {
		Project original = createProject("Editable Project");
		User admin = ensureProjectCapableAdmin();
		String originalName = original.getName();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setName(originalName + " Updated");
		command.setText("Updated project description");
		command.setOrganizationName("Updated Org");
		command = getCommandHandler().execute(command);

		Project updated = command.getProject();
		assertEquals(original.getId(), updated.getId(), "edit should update the same project");
		assertEquals(originalName + " Updated", updated.getName(), "name should be updated");
		assertEquals("Updated project description", updated.getText(), "text should be updated");
		assertEquals("Updated Org", updated.getOrganization().getName(),
				"organization should be updated");
		assertEquals(updated.getId(),
				getProjectRepository().findProjectByName(updated.getName()).getId(),
				"updated project should be reloadable by its new name");
	}

	@Test
	public void editProjectWithMatchingVersionSucceeds() throws Exception {
		Project original = createProject("Version OK Project");
		User admin = ensureProjectCapableAdmin();
		String name = original.getName();
		String org = original.getOrganization().getName();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setName(name);
		command.setText("Updated with the current version");
		command.setOrganizationName(org);
		command.setExpectedVersion(original.getVersion());
		command = getCommandHandler().execute(command);

		assertEquals("Updated with the current version", command.getProject().getText(),
				"matching-version update should be applied");
	}

	@Test
	public void editProjectWithStaleVersionIsRejected() throws Exception {
		Project original = createProject("Version Stale Project");
		User admin = ensureProjectCapableAdmin();
		String name = original.getName();
		String org = original.getOrganization().getName();
		int staleVersion = original.getVersion();

		EditProjectCommand firstEdit = getProjectCommandFactory().newEditProjectCommand();
		firstEdit.setEditedBy(admin);
		firstEdit.setProject(original);
		firstEdit.setName(name);
		firstEdit.setText("First writer's change");
		firstEdit.setOrganizationName(org);
		firstEdit.setExpectedVersion(staleVersion);
		getCommandHandler().execute(firstEdit);

		assertThrows(EntityLockException.class, () -> {
			EditProjectCommand staleEdit = getProjectCommandFactory().newEditProjectCommand();
			staleEdit.setEditedBy(admin);
			staleEdit.setProject(original);
			staleEdit.setName(name);
			staleEdit.setText("Second writer's stale change");
			staleEdit.setOrganizationName(org);
			staleEdit.setExpectedVersion(staleVersion);
			getCommandHandler().execute(staleEdit);
		}, "an update carrying a stale version should be rejected");
	}

	@Test
	public void editProjectRejectsDuplicateName() throws Exception {
		Project first = createProject("Duplicate Source");
		Project second = createProject("Duplicate Target");
		User admin = ensureProjectCapableAdmin();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(second);
		command.setName(first.getName());
		command.setText(second.getText());
		command.setOrganizationName(second.getOrganization().getName());

		assertThrows(EntityException.class, () -> getCommandHandler().execute(command),
				"editing a project to an existing name should fail");
	}

	// -------------------------------------------------------------------------
	// Partial update (issue #316): null leaves a property as it is, "" clears it
	// -------------------------------------------------------------------------

	@Test
	public void editProjectWithNullNameAndNoOrganizationKeepsThem() throws Exception {
		Project original = createProject("Partial Name Project");
		User admin = ensureProjectCapableAdmin();
		String originalName = original.getName();
		String originalOrg = original.getOrganization().getName();

		// A null name also skips the uniqueness lookup, whose finder would NPE on it.
		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setText("Only the description changes");
		command = getCommandHandler().execute(command);

		Project updated = getProjectRepository().get(command.getProject());
		assertEquals(originalName, updated.getName(), "a null name should leave the name as it is");
		assertEquals("Only the description changes", updated.getText(),
				"the supplied description should be applied");
		assertNotNull(updated.getOrganization(),
				"no organization fields should leave the organization as it is");
		assertEquals(originalOrg, updated.getOrganization().getName(),
				"no organization fields should leave the organization as it is");
	}

	@Test
	public void editProjectWithNullDescriptionKeepsDescription() throws Exception {
		Project original = createProject("Partial Text Project");
		User admin = ensureProjectCapableAdmin();
		String originalText = original.getText();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setName(original.getName() + " Renamed");
		command = getCommandHandler().execute(command);

		Project updated = getProjectRepository().get(command.getProject());
		assertEquals(original.getName() + " Renamed", updated.getName(),
				"the supplied name should be applied");
		assertEquals(originalText, updated.getText(),
				"a null description should leave the description as it is");
	}

	@Test
	public void editProjectWithEmptyDescriptionClearsIt() throws Exception {
		Project original = createProject("Clear Text Project");
		User admin = ensureProjectCapableAdmin();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setName(original.getName());
		command.setText("");
		command = getCommandHandler().execute(command);

		Project updated = getProjectRepository().get(command.getProject());
		assertEquals("", updated.getText(), "an empty description should clear the description");
	}

	@Test
	public void editProjectWithEmptyOrganizationNameClearsTheOrganization() throws Exception {
		Project original = createProject("Clear Org Project");
		User admin = ensureProjectCapableAdmin();
		assertNotNull(original.getOrganization(), "fixture should start with an organization");

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(admin);
		command.setProject(original);
		command.setName(original.getName());
		command.setOrganizationName("");
		command = getCommandHandler().execute(command);

		Project updated = getProjectRepository().get(command.getProject());
		assertNull(updated.getOrganization(), "an empty organization name should clear it");
		assertThrows(NoSuchOrganizationException.class,
				() -> getUserRepository().findOrganizationByName(""),
				"clearing must not create an organization with an empty name");
	}

	@Test
	public void createProjectWithEmptyOrganizationNameHasNoOrganization() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		User creator = ensureProjectCapableAdmin();

		EditProjectCommand command = getProjectCommandFactory().newEditProjectCommand();
		command.setEditedBy(creator);
		command.setName("No Org Project " + uniqueifier);
		command.setText("Created without an organization");
		command.setOrganizationName("");
		command = getCommandHandler().execute(command);

		assertNull(command.getProject().getOrganization(),
				"an empty organization name should mean no organization");
		assertThrows(NoSuchOrganizationException.class,
				() -> getUserRepository().findOrganizationByName(""),
				"creating must not create an organization with an empty name");
	}
}
