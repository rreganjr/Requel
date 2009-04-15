/*
 * $Id: ConvertStepToScenarioCommandImpl.java,v 1.6 2009/03/30 11:54:28 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.command.ConvertStepToScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;

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
