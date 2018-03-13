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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editScenarioCommand")
@Scope("prototype")
public class EditScenarioCommandImpl extends EditScenarioStepCommandImpl implements
		EditScenarioCommand {

	private List<EditScenarioStepCommand> editStepCommands;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditScenarioCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public Scenario getScenario() {
		return (Scenario) getStep();
	}

	@Override
	public void setScenario(Scenario scenario) {
		setStep(scenario);
	}

	@Override
	public void setStepCommands(List<EditScenarioStepCommand> editStepCommands) {
		this.editStepCommands = editStepCommands;
	}

	protected List<EditScenarioStepCommand> getStepCommands() {
		return editStepCommands;
	}

	@Override
	public void execute() throws Exception {
		List<EditScenarioStepCommand> stepEditCommands = new ArrayList<EditScenarioStepCommand>(
				getStepCommands());
		for (int index = 0; index < stepEditCommands.size(); index++) {
			EditScenarioStepCommand stepCommand = stepEditCommands.get(index);
			// disable analysis of the steps as they will get analyzed
			// when the whole scenario is analyzed.
			stepCommand.setAnalysisEnabled(false);
			stepEditCommands.set(index, getCommandHandler().execute(stepCommand));
		}
		User editedBy = getProjectRepository().get(getEditedBy());
		ScenarioImpl scenarioImpl = (ScenarioImpl) getScenario();
		ProjectOrDomain projectOrDomain = getRepository().get(getProjectOrDomain());

		// check for uniqueness
		try {
			Scenario existing = getProjectRepository().findScenarioByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (scenarioImpl == null) {
				throw EntityException.uniquenessConflict(Scenario.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(scenarioImpl)) {
				throw EntityException.uniquenessConflict(Scenario.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (scenarioImpl == null) {
			scenarioImpl = getProjectRepository().persist(
					new ScenarioImpl(projectOrDomain, editedBy, getName(), getText(),
							getScenarioType()));
		} else {
			scenarioImpl.setName(getName());
			scenarioImpl.setText(getText());
			scenarioImpl.setType(getScenarioType());
		}
		// TODO: merge is failing because the use cases in the
		// usedByUseCases property are my proxies and hibernate throws an
		// IllegalArgumentException: Unknown entity:
		// com.rreganjr.requel.project.impl.UseCaseImpl$$EnhancerByCGLIB$$b1ba12b8
		for (Scenario usingScenario : scenarioImpl.getUsingScenarios()) {
			usingScenario = getRepository().get(usingScenario);
		}

		scenarioImpl = getProjectRepository().merge(scenarioImpl);
		scenarioImpl.getSteps().clear();
		for (EditScenarioStepCommand executedCommand : stepEditCommands) {
			Step step = executedCommand.getStep();
			scenarioImpl.getSteps().add(step);
			step.getUsingScenarios().add(scenarioImpl);
		}
		setScenario(getProjectRepository().merge(scenarioImpl));
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeScenario(getScenario());
		}
	}
}
