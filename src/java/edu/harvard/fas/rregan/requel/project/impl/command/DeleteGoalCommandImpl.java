/*
 * $Id$
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalRelationCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.command.RemoveGoalFromGoalContainerCommand;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
