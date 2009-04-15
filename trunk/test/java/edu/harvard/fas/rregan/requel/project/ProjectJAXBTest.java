package edu.harvard.fas.rregan.requel.project;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.EditIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.command.ResolveIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.IssueImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.NoteImpl;
import edu.harvard.fas.rregan.requel.project.command.EditGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.project.impl.ProjectImpl;
import edu.harvard.fas.rregan.requel.user.User;

public class ProjectJAXBTest extends AbstractIntegrationTestCase {

	public void testJaxb() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		String projectDescription = "This is a test project " + uniqueifier;
		String organizationName = "Test Organization " + uniqueifier;
		User creator = getUserRepository().findUserByUsername("project");
		EditProjectCommand editProjectCommand = getProjectCommandFactory().newEditProjectCommand();
		editProjectCommand.setEditedBy(creator);
		editProjectCommand.setName(projectName);
		editProjectCommand.setText(projectDescription);
		editProjectCommand.setOrganizationName(organizationName);
		editProjectCommand = getCommandHandler().execute(editProjectCommand);
		Project project = editProjectCommand.getProject();

		createGoal(
				"access from the Virgin XL WAP",
				"Users will access the Streaming Video service via a link from the Virgin XL WAP environment.",
				creator, project);
		createGoal("refer content to a friend",
				"The EVDO Video Product must allow the user to refer content to a friend via SMS.",
				creator, project);
		createGoal(
				"Main Menu components",
				"The EVDO Streaming Video Main Menu should contain: * Featured Content * VMU Channels: Life & Hot Shots * Catalog Browse * Top Rated Content in the Community *	Top Viewed Content in the Community * VMU-branded content channels that include the Nellymoser Sourced Content.",
				creator, project);
		createGoal(
				"Main Menu flow",
				"The EVDO Streaming Video Main Menu user flow is: Main Menu -> Content Folder or Browse Results Page -> Video Clip Details Page -> Purchase Confirmation (If applicable) -> Stream -> Video Clip Return Page -> Originating Content Folder or Browse Results Page.",
				creator, project);
		Goal goal = createGoal(
				"Video Clip Details Page",
				"The Video Clip Details Page will include user Poll Results, Community Rating and User Comments.",
				creator, project);

		Issue issue = createIssue(project, goal, "The thing-a-ma-bob has the wrong whosy-whatsy",
				creator);
		Position position = createPosition(issue, "Don't do anything.", creator);
		ResolveIssueCommand resolveIssueCommand = getAnnotationCommandFactory()
				.newResolveIssueCommand(position);
		resolveIssueCommand.setPosition(position);
		resolveIssueCommand.setIssue(issue);
		resolveIssueCommand.setEditedBy(creator);
		resolveIssueCommand = getCommandHandler().execute(resolveIssueCommand);

		issue = resolveIssueCommand.getIssue();
		goal = (Goal) issue.getAnnotatables().iterator().next();
		project = (Project) goal.getProjectOrDomain();
		try {
			StringWriter writer = new StringWriter();
			// NOTE: the annotation classes need to be explicitly supplied to
			// the newInstance or an error will occur:
			// IllegalAnnotationExceptions
			// at public java.util.Set
			// edu.harvard.fas.rregan.requel.project.impl.AbstractProjectOrDomainEntity.getAnnotations()
			JAXBContext context = JAXBContext.newInstance(ProjectImpl.class, NoteImpl.class,
					IssueImpl.class);
			Marshaller m = context.createMarshaller();
			m.marshal(project, writer);
			System.out.println(writer);
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		}
	}

	private Goal createGoal(String name, String text, User creator, Project project)
			throws Exception {
		EditGoalCommand editGoalCommand = getProjectCommandFactory().newEditGoalCommand();
		editGoalCommand.setAnalysisEnabled(false);
		editGoalCommand.setEditedBy(creator);
		editGoalCommand.setGoalContainer(project);
		editGoalCommand.setName(name);
		editGoalCommand.setText(text);
		editGoalCommand = getCommandHandler().execute(editGoalCommand);
		return editGoalCommand.getGoal();
	}

	private Issue createIssue(Object groupingObject, Annotatable annotatable, String text,
			User creator) throws Exception {
		EditIssueCommand editIssueCommand = getAnnotationCommandFactory().newEditIssueCommand();
		editIssueCommand.setGroupingObject(groupingObject);
		editIssueCommand.setAnnotatable(annotatable);
		editIssueCommand.setEditedBy(creator);
		editIssueCommand.setMustBeResolved(true);
		editIssueCommand.setText(text);
		editIssueCommand = getCommandHandler().execute(editIssueCommand);
		return editIssueCommand.getIssue();
	}

	private Position createPosition(Issue issue, String text, User creator) throws Exception {
		EditPositionCommand editPositionCommand = getAnnotationCommandFactory()
				.newEditPositionCommand();
		editPositionCommand.setIssue(issue);
		editPositionCommand.setEditedBy(creator);
		editPositionCommand.setText(text);
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

}
