package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.user.User;

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
			if (creator.equals(stakeholder.getUser())) {
				expectedStakeholders.add(stakeholder);
			}
		}
		assertEquals(creator, project.getCreatedBy());
		assertEquals(projectName, project.getName());
		assertEquals(projectDescription, project.getDescription());
		assertEquals(organizationName, project.getOrganization().getName());
		assertEquals(expectedStakeholders, project.getStakeholders());
	}
}
