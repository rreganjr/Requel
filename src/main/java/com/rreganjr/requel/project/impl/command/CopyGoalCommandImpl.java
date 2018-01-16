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
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.CopyGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("copyGoalCommand")
@Scope("prototype")
public class CopyGoalCommandImpl extends AbstractEditProjectCommand implements CopyGoalCommand {

	private Goal originalGoal;
	private Goal newGoal;
	private String newGoalName;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public CopyGoalCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	protected Goal getOriginalGoal() {
		return originalGoal;
	}

	public void setOriginalGoal(Goal originalGoal) {
		this.originalGoal = originalGoal;
	}

	protected String getNewGoalName() {
		return newGoalName;
	}

	public void setNewGoalName(String newGoalName) {
		this.newGoalName = newGoalName;
	}

	@Override
	public Goal getNewGoal() {
		return newGoal;
	}

	protected void setNewGoal(Goal newGoal) {
		this.newGoal = newGoal;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		GoalImpl originalGoal = (GoalImpl) getRepository().get(getOriginalGoal());

		String newName;
		if ((getNewGoalName() == null) || (getNewGoalName().length() == 0)) {
			newName = generateNewGoalName(originalGoal.getName());
		} else {
			newName = generateNewGoalName(getNewGoalName());
		}
		GoalImpl newGoal = getProjectRepository().persist(
				new GoalImpl(originalGoal.getProjectOrDomain(), editedBy, newName, originalGoal
						.getText()));

		for (GoalRelation goalRelation : originalGoal.getRelationsFromThisGoal()) {
			EditGoalRelationCommand editGoalRelationCommand = getProjectCommandFactory()
					.newEditGoalRelationCommand();
			editGoalRelationCommand.setEditedBy(editedBy);
			editGoalRelationCommand.setFromGoal(newGoal.getName());
			editGoalRelationCommand.setToGoal(goalRelation.getToGoal().getName());
			editGoalRelationCommand.setProjectOrDomain(newGoal.getProjectOrDomain());
			editGoalRelationCommand.setRelationType(goalRelation.getRelationType().name());
			editGoalRelationCommand = getCommandHandler().execute(editGoalRelationCommand);
			newGoal.getRelationsFromThisGoal().add(editGoalRelationCommand.getGoalRelation());
		}
		// TODO: this assumes that all annotations are appropriate for the new
		// goal
		for (Annotation annotation : originalGoal.getAnnotations()) {
			newGoal.getAnnotations().add(annotation);
			annotation.getAnnotatables().add(newGoal);
		}
		for (GlossaryTerm term : originalGoal.getGlossaryTerms()) {
			newGoal.getGlossaryTerms().add(term);
			term.getReferers().add(newGoal);
		}
		newGoal = getProjectRepository().merge(newGoal);
		setNewGoal(newGoal);
	}

	private String generateNewGoalName(String originalName) {
		String newName = originalName;
		try {
			getProjectRepository().findGoalByProjectOrDomainAndName(
					getOriginalGoal().getProjectOrDomain(), newName);
			int counter = 1;
			newName = originalName + " " + counter;
			while (true) {
				try {
					getProjectRepository().findGoalByProjectOrDomainAndName(
							getOriginalGoal().getProjectOrDomain(), newName);
					counter++;
					newName = originalName + " " + counter;
				} catch (EntityException e) {
					// new name is not in use
					break;
				}
			}
		} catch (EntityException e) {
			// new name is not in use
		}
		return newName;
	}
}
