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
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a stakeholder from a project, cleaning up references from other
 * project entities, stakeholder relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteStakeholderCommand")
@Scope("prototype")
public class DeleteStakeholderCommandImpl extends AbstractEditProjectCommand implements
		DeleteStakeholderCommand {

	private Stakeholder stakeholder;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteStakeholderCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setStakeholder(Stakeholder stakeholder) {
		this.stakeholder = stakeholder;
	}

	protected Stakeholder getStakeholder() {
		return stakeholder;
	}

	@Override
	public void execute() throws Exception {
		Stakeholder stakeholder = getRepository().get(getStakeholder());
		Set<Annotation> annotations = new HashSet<Annotation>(stakeholder.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(getEditedBy());
			removeAnnotationFromAnnotatableCommand.setAnnotatable(stakeholder);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : stakeholder.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(stakeholder)) {
				term.getReferers().remove(stakeholder);
			}
		}
		stakeholder.getProjectOrDomain().getStakeholders().remove(stakeholder);
		if (stakeholder.isUserStakeholder()) {
			UserStakeholder userStakeholder = (UserStakeholder) stakeholder;
			userStakeholder.getUser().getRoleForType(ProjectUserRole.class).getActiveProjects()
					.remove(userStakeholder.getProjectOrDomain());
			if (userStakeholder.getTeam() != null) {
				userStakeholder.getTeam().getMembers().remove(userStakeholder);
			}
		}
		for (GlossaryTerm term : stakeholder.getProjectOrDomain().getGlossaryTerms()) {
			term.getReferers().remove(stakeholder);
		}
		for (Goal goal : stakeholder.getGoals()) {
			goal.getReferers().remove(stakeholder);
		}
		getRepository().delete(stakeholder);
	}
}
