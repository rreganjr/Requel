package com.rreganjr.requel.project.impl.assistant;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import org.junit.Assert;

/**
 * Test the GoalAssistant
 * 
 * @author ron
 */
public class GoalAssistantTest extends AbstractIntegrationTestCase {

	/**
	 * Test the goal assistant with a spelling error in the goal name.
	 * 
	 * @throws Exception
	 */
	public void testGoalAssistantGoalNameIssue() throws Exception {
		long uniqueifier = System.currentTimeMillis();
		String projectName = "Test Project " + uniqueifier;
		String organizationName = "Test Organization " + uniqueifier;
		User creator = getUserRepository().findUserByUsername("project");
		EditProjectCommand editProjectCommand = getProjectCommandFactory().newEditProjectCommand();
		editProjectCommand.setEditedBy(creator);
		editProjectCommand.setName(projectName);
		editProjectCommand.setOrganizationName(organizationName);
		editProjectCommand = getCommandHandler().execute(editProjectCommand);
		Project project = editProjectCommand.getProject();

		EditGoalCommand editGoalCommand = getProjectCommandFactory().newEditGoalCommand();
		String goalName = "Test groal " + uniqueifier;
		editGoalCommand.setEditedBy(creator);
		editGoalCommand.setGoalContainer(project);
		editGoalCommand.setName(goalName);
		editGoalCommand.setText("new content must be distinguished "
				+ "from archive content with a tag or other visual marker.");
		editGoalCommand = getCommandHandler().execute(editGoalCommand);
		Goal goal = editGoalCommand.getGoal();

		LexicalAssistant lexicalAssistant = new LexicalAssistant(getCommandHandler(),
				getProjectCommandFactory(), getAnnotationCommandFactory(),
				getAnnotationRepository(), getProjectRepository(), getDictionaryRepository(),
				getNlpProcessorFactory());

		GoalAssistant assistant = new GoalAssistant(lexicalAssistant, creator);
		assistant.setEntity(goal);
		assistant.analyze();

		// check for annotations
		// assertEquals(1, goal.getAnnotations().size());
		for (Annotation annotation : goal.getAnnotations()) {
			if ((annotation instanceof Issue) && annotation.getText().contains("'groal'")) {
				Assert.assertEquals(
						"The word 'groal' in the goal name is not recognized and may be spelled incorrectly.",
						annotation.getText());
			}
		}
	}
}
