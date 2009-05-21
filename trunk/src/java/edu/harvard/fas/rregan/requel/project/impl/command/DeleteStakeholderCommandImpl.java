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
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.command.DeleteStakeholderCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
		if (stakeholder.getUser() != null) {
			stakeholder.getUser().getRoleForType(ProjectUserRole.class).getActiveProjects().remove(
					stakeholder.getProjectOrDomain());
		}
		if (stakeholder.getTeam() != null) {
			stakeholder.getTeam().getMembers().remove(stakeholder);
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
