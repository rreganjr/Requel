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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.ConvertStepToScenarioCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
public class ConvertStepToScenarioCommandImpl extends EditScenarioCommandImpl implements
		ConvertStepToScenarioCommand {

	Step originalStep;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public ConvertStepToScenarioCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setOriginalScenarioStep(Step originalStep) {
		this.originalStep = originalStep;

	}

	protected Step getOriginalScenarioStep() {
		return originalStep;
	}

	@Override
	public void execute() throws Exception {
		Map<Scenario, Integer> stepInScenarioMap = new HashMap<Scenario, Integer>();
		Set<Annotation> stepAnnotations = new HashSet<Annotation>();
		if (getOriginalScenarioStep() != null) {
			// remove the original step from scenarios and annotations
			// and save them so they can be added to the new scenario and
			// vice versa
			for (Scenario scenario : getOriginalScenarioStep().getUsingScenarios()) {
				scenario = getRepository().get(scenario);
				for (int index = 0; index < scenario.getSteps().size(); index++) {
					if (getOriginalScenarioStep().equals(scenario.getSteps().get(index))) {
						stepInScenarioMap.put(scenario, index);
						scenario.getSteps().remove(index);
						break;
					}
				}
			}
			for (Annotation annotation : getOriginalScenarioStep().getAnnotations()) {
				annotation = getRepository().get(annotation);
				annotation.getAnnotatables().remove(getOriginalScenarioStep());
				stepAnnotations.add(annotation);
			}
			getRepository().delete(getOriginalScenarioStep());
			getRepository().flush();
		}
		// use the super-command to create the new scenario
		super.execute();
		Scenario newScenario = getScenario();

		// TODO: this assume the scenarios that refered to the step haven't
		// changed since this command started executing
		for (Scenario scenario : stepInScenarioMap.keySet()) {
			scenario = getRepository().get(scenario);
			Integer index = stepInScenarioMap.get(scenario);
			if (index >= scenario.getSteps().size()) {
				scenario.getSteps().add(newScenario);
			} else {
				scenario.getSteps().add(index, newScenario);
			}
		}

		for (Annotation annotation : stepAnnotations) {
			annotation.getAnnotatables().add(newScenario);
			newScenario.getAnnotations().add(annotation);
		}
	}
}
