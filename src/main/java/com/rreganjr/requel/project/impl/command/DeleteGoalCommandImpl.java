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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteGoalRelationCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a goal from a project, cleaning up references from other project
 * entities, goal relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteGoalCommand")
@Scope("prototype")
public class DeleteGoalCommandImpl extends AbstractEditProjectCommand implements DeleteGoalCommand {

	private Goal goal;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteGoalCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	protected Goal getGoal() {
		return goal;
	}

	@Override
	public void execute() throws Exception {
		Goal goal = getRepository().get(getGoal());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(goal.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(goal);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : goal.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(goal)) {
				term.getReferers().remove(goal);
			}
		}
		Set<GoalContainer> goalReferers = new HashSet<GoalContainer>(goal.getReferers());
		for (GoalContainer goalContainer : goalReferers) {
			RemoveGoalFromGoalContainerCommand removeGoalFromGoalContainerCommand = getProjectCommandFactory()
					.newRemoveGoalFromGoalContainerCommand();
			removeGoalFromGoalContainerCommand.setEditedBy(editedBy);
			removeGoalFromGoalContainerCommand.setGoal(goal);
			removeGoalFromGoalContainerCommand.setGoalContainer(goalContainer);
			getCommandHandler().execute(removeGoalFromGoalContainerCommand);
		}
		goal.getProjectOrDomain().getGoals().remove(goal);
		Set<GoalRelation> goalRelations = new HashSet<GoalRelation>(goal.getRelationsFromThisGoal());
		goalRelations.addAll(goal.getRelationsToThisGoal());
		for (GoalRelation goalRelation : goalRelations) {
			DeleteGoalRelationCommand deleteGoalRelationCommand = getProjectCommandFactory()
					.newDeleteGoalRelationCommand();
			deleteGoalRelationCommand.setEditedBy(editedBy);
			deleteGoalRelationCommand.setGoalRelation(goalRelation);
			getCommandHandler().execute(deleteGoalRelationCommand);
		}
		getRepository().delete(goal);
	}

}
