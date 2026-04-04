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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class EditProjectCommandImplTest extends AbstractIntegrationTestCase {

	@Test
	public void testProjectCreation() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		String projectDescription = "This is a test project " + uniqueifier;
		String organizationName = "Text Organization " + uniqueifier;
		User creator = getUserRepository().findUserByUsername("admin");
		// Ensure admin can hold projects for this test (persisted via command to honor validators).
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
			creator = getUserRepository().findUserByUsername("admin"); // reload with granted role
		}
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
		Assert.assertEquals(creator, project.getCreatedBy());
		Assert.assertEquals(projectName, project.getName());
		Assert.assertEquals("Project: " + projectName, project.getDescription());
		Assert.assertEquals(organizationName, project.getOrganization().getName());
		Assert.assertTrue(project.getStakeholders().containsAll(expectedStakeholders));
	}
}
