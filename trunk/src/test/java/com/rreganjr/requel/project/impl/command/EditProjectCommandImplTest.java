package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import org.junit.Assert;

public class EditProjectCommandImplTest extends AbstractIntegrationTestCase {

	public void testProjectCreation() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		String projectDescription = "This is a test project " + uniqueifier;
		String organizationName = "Text Organization " + uniqueifier;
		User creator = getUserRepository().findUserByUsername("admin");
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
		Assert.assertEquals(projectDescription, project.getDescription());
		Assert.assertEquals(organizationName, project.getOrganization().getName());
		Assert.assertEquals(expectedStakeholders, project.getStakeholders());
	}
}
