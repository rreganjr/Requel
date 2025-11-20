/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.project.impl.assistant;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.platform.identity.User;
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
