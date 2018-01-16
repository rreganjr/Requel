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
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editGoalCommand")
@Scope("prototype")
public class EditGoalCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditGoalCommand {

	private GoalContainer goalContainer;
	private Goal goal;
	private String text;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditGoalCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public Goal getGoal() {
		return goal;
	}

	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	@Override
	public void setGoalContainer(GoalContainer goalContainer) {
		this.goalContainer = goalContainer;
	}

	protected GoalContainer getGoalContainer() {
		return goalContainer;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	@Override
	public void execute() {
		GoalContainer goalContainer = getProjectRepository().get(getGoalContainer());
		User editedBy = getProjectRepository().get(getEditedBy());
		GoalImpl goalImpl = (GoalImpl) getGoal();
		ProjectOrDomain projectOrDomain = null;
		if (goalImpl != null) {
			projectOrDomain = goalImpl.getProjectOrDomain();
		} else if (goalContainer != null) {
			if (goalContainer instanceof ProjectOrDomain) {
				projectOrDomain = (ProjectOrDomain) goalContainer;
			} else if (goalContainer instanceof ProjectOrDomainEntity) {
				projectOrDomain = ((ProjectOrDomainEntity) goalContainer).getProjectOrDomain();
			} else {
				// TODO: error?
			}
		}
		projectOrDomain = getRepository().get(projectOrDomain);

		// check for uniqueness
		try {
			Goal existing = getProjectRepository().findGoalByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (goalImpl == null) {
				throw EntityException.uniquenessConflict(Goal.class, existing,
						EditGoalCommand.FIELD_NAME, EntityExceptionActionType.Creating);
			} else if (!existing.equals(goalImpl)) {
				throw EntityException.uniquenessConflict(Goal.class, existing,
						EditGoalCommand.FIELD_NAME, EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (goalImpl == null) {
			goalImpl = getProjectRepository().persist(
					new GoalImpl(projectOrDomain, editedBy, getName(), getText()));
		} else {
			goalImpl.setName(getName());
			goalImpl.setText(getText());
		}
		if (goalContainer != null) {
			goalImpl.getReferers().add(goalContainer);
		}
		setGoal(getProjectRepository().merge(goalImpl));
		if (goalContainer != null) {
			goalContainer.getGoals().add(goalImpl);
		}
		setGoalContainer(goalContainer);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeGoal(getGoal());
		}
	}
}
