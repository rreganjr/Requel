/*
 * $Id: AddGoalToGoalContainerCommandImpl.java,v 1.10 2009/03/30 11:54:28 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.AddGoalToGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("addGoalToGoalContainerCommand")
@Scope("prototype")
public class AddGoalToGoalContainerCommandImpl extends AbstractEditProjectCommand implements
		AddGoalToGoalContainerCommand {

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public AddGoalToGoalContainerCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository, 
			ProjectCommandFactory projectCommandFactory, AnnotationCommandFactory annotationCommandFactory,
			CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory, annotationCommandFactory, commandHandler);
	}

	private Goal goal;
	private GoalContainer goalContainer;

	@Override
	public Goal getGoal() {
		return goal;
	}

	@Override
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	@Override
	public GoalContainer getGoalContainer() {
		return goalContainer;
	}

	@Override
	public void setGoalContainer(GoalContainer goalContainer) {
		this.goalContainer = goalContainer;
	}

	@Override
	public void execute() {
		Goal addedGoal = getProjectRepository().get(getGoal());
		GoalContainer addingContainer = getProjectRepository().get(getGoalContainer());
		addedGoal.getReferers().add(addingContainer);
		addingContainer.getGoals().add(addedGoal);

		// replaced the supplied objects with the updated objects for retrieval.
		addedGoal = getProjectRepository().merge(addedGoal);
		addingContainer = getProjectRepository().merge(addingContainer);
		setGoal(addedGoal);
		setGoalContainer(addingContainer);
	}
}
