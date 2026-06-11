/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.project.command.DeleteGoalRelationCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("deleteGoalRelationCommand")
@Scope("prototype")
public class DeleteGoalRelationCommandImpl extends AbstractEditProjectCommand implements
		DeleteGoalRelationCommand, ProjectScopedCommand, AuthorizableCommand {

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(com.rreganjr.requel.project.Goal.class, "Edit");
	}

	private GoalRelation goalRelation;

	@Autowired
	public DeleteGoalRelationCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setGoalRelation(GoalRelation goalRelation) {
		this.goalRelation = goalRelation;
	}

	protected GoalRelation getGoalRelation() {
		return goalRelation;
	}

	@Override
	public void execute() throws Exception {
		GoalRelation goalRelation = getRepository().get(getGoalRelation());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(goalRelation.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(goalRelation);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		goalRelation.getFromGoal().getRelationsFromThisGoal().remove(goalRelation);
		goalRelation.getToGoal().getRelationsToThisGoal().remove(goalRelation);
		getRepository().delete(goalRelation);
	}

	@Override
	public Project getProject() {
		// GoalRelation lives in the same Project as its endpoints; both fromGoal
		// and toGoal must belong to the same Project, so either side resolves
		// to the right one.
		if (goalRelation != null) {
			if (goalRelation.getFromGoal() != null
					&& goalRelation.getFromGoal().getProjectOrDomain() instanceof Project project) return project;
			if (goalRelation.getToGoal() != null
					&& goalRelation.getToGoal().getProjectOrDomain() instanceof Project project) return project;
		}
		return null;
	}

}
