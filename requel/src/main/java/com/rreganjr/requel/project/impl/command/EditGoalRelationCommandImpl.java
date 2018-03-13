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
import com.rreganjr.requel.EntityValidationException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.GoalRelationType;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.GoalSelfRelationException;
import com.rreganjr.requel.project.impl.GoalRelationImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editGoalRelationCommand")
@Scope("prototype")
public class EditGoalRelationCommandImpl extends AbstractProjectCommand implements
		EditGoalRelationCommand {

	private ProjectOrDomain projectOrDomain;
	private GoalRelation goalRelation;
	private String fromGoalName;
	private String toGoalName;
	private String relationTypeName;
	private User editedBy;
	private boolean analysisEnabled = true;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 */
	@Autowired
	public EditGoalRelationCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditGoalRelationCommand#getGoalRelation()
	 */
	@Override
	public GoalRelation getGoalRelation() {
		return goalRelation;
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditGoalRelationCommand#setGoalRelation(com.rreganjr.requel.project.GoalRelation)
	 */
	@Override
	public void setGoalRelation(GoalRelation goalRelation) {
		this.goalRelation = goalRelation;
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditGoalRelationCommand#setFromGoal(java.lang.String)
	 */
	@Override
	public void setFromGoal(String goalName) {
		this.fromGoalName = goalName;
	}

	protected String getFromGoal() {
		return fromGoalName;
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditGoalRelationCommand#setRelationType(java.lang.String)
	 */
	@Override
	public void setRelationType(String relationTypeName) {
		this.relationTypeName = relationTypeName;
	}

	protected String getRelationType() {
		return relationTypeName;
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditGoalRelationCommand#setToGoal(java.lang.String)
	 */
	@Override
	public void setToGoal(String goalName) {
		this.toGoalName = goalName;
	}

	protected String getToGoal() {
		return toGoalName;
	}

	@Override
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain) {
		this.projectOrDomain = projectOrDomain;
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return projectOrDomain;
	}

	/**
	 * @see com.rreganjr.requel.command.EditCommand#setEditedBy(com.rreganjr.requel.user.User)
	 */
	@Override
	public void setEditedBy(User user) {
		this.editedBy = user;
	}

	protected User getEditedBy() {
		return editedBy;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() {
		User editedBy = getRepository().get(getEditedBy());
		ProjectOrDomain projectOrDomain = getRepository().get(getProjectOrDomain());
		GoalRelationImpl goalRelationImpl = (GoalRelationImpl) getGoalRelation();
		Goal toGoal = getProjectRepository().findGoalByProjectOrDomainAndName(getProjectOrDomain(),
				getToGoal());
		Goal fromGoal = getProjectRepository().findGoalByProjectOrDomainAndName(
				getProjectOrDomain(), getFromGoal());
		GoalRelationType goalRelationType;
		try {
			goalRelationType = GoalRelationType.valueOf(getRelationType());
		} catch (Exception e) {
			throw EntityValidationException.validationFailed(GoalRelation.class, "relationType",
					"The goal relation type cannot be " + getRelationType());
		}

		if (toGoal.equals(fromGoal)) {
			throw GoalSelfRelationException.forGoal(toGoal);
		}
		// TODO: check for uniqueness?
		if (goalRelationImpl == null) {
			goalRelationImpl = getProjectRepository().persist(
					new GoalRelationImpl(fromGoal, toGoal, goalRelationType, editedBy));
		} else {
			goalRelationImpl.setFromGoal(fromGoal);
			goalRelationImpl.setToGoal(toGoal);
			goalRelationImpl.setRelationType(goalRelationType);
			goalRelationImpl = getProjectRepository().merge(goalRelationImpl);
		}
		setGoalRelation(goalRelationImpl);
	}

	@Override
	public void setAnalysisEnabled(boolean analysisEnabled) {
		this.analysisEnabled = analysisEnabled;
	}

	protected boolean isAnalysisEnabled() {
		return analysisEnabled;
	}

	@Override
	public void invokeAnalysis() {
		// TODO: do goal relations need to be analyzed?
	}
}
