package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.AbstractIntegrationTestCase;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.command.EditGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.user.User;

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
				assertEquals(
						"The word 'groal' in the goal name is not recognized and may be spelled incorrectly.",
						annotation.getText());
			}
		}
	}
}
