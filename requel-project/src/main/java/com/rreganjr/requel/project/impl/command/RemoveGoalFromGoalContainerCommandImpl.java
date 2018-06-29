/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("removeGoalFromGoalContainerCommand")
@Scope("prototype")
public class RemoveGoalFromGoalContainerCommandImpl extends AbstractEditProjectCommand implements
		RemoveGoalFromGoalContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public RemoveGoalFromGoalContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private Goal goal;
	private GoalContainer goalContainer;

	@Override
	public Goal getGoal() {
		return goal;
	}

	@Override
	public GoalContainer getGoalContainer() {
		return goalContainer;
	}

	@Override
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	@Override
	public void setGoalContainer(GoalContainer goalContainer) {
		this.goalContainer = goalContainer;
	}

	@Override
	public void execute() {
		Goal removedGoal = getProjectRepository().get(getGoal());
		GoalContainer removingContainer = getProjectRepository().get(getGoalContainer());
		removedGoal.getReferrers().remove(removingContainer);
		removingContainer.getGoals().remove(removedGoal);

		// replaced the supplied objects with the updated objects for retrieval.
		removedGoal = getRepository().merge(removedGoal);
		removingContainer = getRepository().merge(removingContainer);
		setGoal(removedGoal);
		setGoalContainer(removingContainer);
	}
}
