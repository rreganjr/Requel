/*
 * $Id: DeleteScenarioCommandImpl.java,v 1.4 2009/03/30 11:54:29 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.DeleteScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * Delete a scenario from a project, cleaning up references from other project
 * entities, scenario relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteScenarioCommand")
@Scope("prototype")
public class DeleteScenarioCommandImpl extends AbstractEditProjectCommand implements
		DeleteScenarioCommand {

	private Scenario scenario;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteScenarioCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	protected Scenario getScenario() {
		return scenario;
	}

	@Override
	public void execute() throws Exception {
		Scenario scenario = getRepository().get(getScenario());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(scenario.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(scenario);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : scenario.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(scenario)) {
				term.getReferers().remove(scenario);
			}
		}
		Set<Scenario> scenarioReferers = new HashSet<Scenario>(scenario.getUsingScenarios());
		for (Scenario scenarioReferer : scenarioReferers) {
			scenarioReferer.getSteps().remove(scenario);
		}
		Set<UseCase> scenarioUseCaseReferers = new HashSet<UseCase>(scenario.getUsingUseCases());
		for (UseCase usecase : scenarioUseCaseReferers) {
			// TODO: delete the usecase or set a new empty scenario on the
			// usecase.
		}
		scenario.getProjectOrDomain().getScenarios().remove(scenario);
		getRepository().delete(scenario);
	}

}
