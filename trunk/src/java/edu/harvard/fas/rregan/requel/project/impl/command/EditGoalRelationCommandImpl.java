/*
 * $Id: EditGoalRelationCommandImpl.java,v 1.12 2009/03/30 11:54:31 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.GoalRelationType;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.exception.GoalSelfRelationException;
import edu.harvard.fas.rregan.requel.project.impl.GoalRelationImpl;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
	 * @see edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand#getGoalRelation()
	 */
	@Override
	public GoalRelation getGoalRelation() {
		return goalRelation;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand#setGoalRelation(edu.harvard.fas.rregan.requel.project.GoalRelation)
	 */
	@Override
	public void setGoalRelation(GoalRelation goalRelation) {
		this.goalRelation = goalRelation;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand#setFromGoal(java.lang.String)
	 */
	@Override
	public void setFromGoal(String goalName) {
		this.fromGoalName = goalName;
	}

	protected String getFromGoal() {
		return fromGoalName;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand#setRelationType(java.lang.String)
	 */
	@Override
	public void setRelationType(String relationTypeName) {
		this.relationTypeName = relationTypeName;
	}

	protected String getRelationType() {
		return relationTypeName;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.project.command.EditGoalRelationCommand#setToGoal(java.lang.String)
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
	 * @see edu.harvard.fas.rregan.requel.command.EditCommand#setEditedBy(edu.harvard.fas.rregan.requel.user.User)
	 */
	@Override
	public void setEditedBy(User user) {
		this.editedBy = user;
	}

	protected User getEditedBy() {
		return editedBy;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
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
