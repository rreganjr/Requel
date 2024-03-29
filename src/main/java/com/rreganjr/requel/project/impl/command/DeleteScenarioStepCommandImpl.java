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
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a scenarioStep from a project, cleaning up references from other
 * project entities, scenarioStep relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteScenarioStepCommand")
@Scope("prototype")
public class DeleteScenarioStepCommandImpl extends AbstractEditProjectCommand implements
		DeleteScenarioStepCommand {

	private Step scenarioStep;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteScenarioStepCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setScenarioStep(Step scenarioStep) {
		this.scenarioStep = scenarioStep;
	}

	protected Step getScenarioStep() {
		return scenarioStep;
	}

	@Override
	public void execute() throws Exception {
		Step scenarioStep = getRepository().get(getScenarioStep());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(scenarioStep.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(scenarioStep);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : scenarioStep.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(scenarioStep)) {
				term.getReferers().remove(scenarioStep);
			}
		}
		Set<Scenario> scenarioReferers = new HashSet<Scenario>(scenarioStep.getUsingScenarios());
		for (Scenario scenarioReferer : scenarioReferers) {
			scenarioReferer.getSteps().remove(scenarioStep);
		}
		getRepository().delete(scenarioStep);
	}

}
