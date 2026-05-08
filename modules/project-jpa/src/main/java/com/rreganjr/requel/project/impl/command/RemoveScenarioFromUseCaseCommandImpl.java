/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveScenarioFromUseCaseCommand;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

@Controller("removeScenarioFromUseCaseCommand")
@Scope("prototype")
public class RemoveScenarioFromUseCaseCommandImpl extends AbstractEditProjectCommand
		implements RemoveScenarioFromUseCaseCommand, ProjectScopedCommand {

	@Autowired
	public RemoveScenarioFromUseCaseCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	private UseCase useCase;
	private Scenario scenario;

	@Override public UseCase getUseCase() { return useCase; }
	@Override public void setUseCase(UseCase useCase) { this.useCase = useCase; }
	@Override public Scenario getScenario() { return scenario; }
	@Override public void setScenario(Scenario scenario) { this.scenario = scenario; }

	@Override
	public void execute() {
		UseCaseImpl useCaseImpl = (UseCaseImpl) getProjectRepository().get(getUseCase());
		ScenarioImpl scenarioImpl = (ScenarioImpl) getProjectRepository().get(getScenario());
		useCaseImpl.getAdditionalScenarios().remove(scenarioImpl);
		getRepository().merge(useCaseImpl);
		setUseCase(useCaseImpl);
	}

	@Override
	public Project getProject() {
		if (useCase != null && useCase.getProjectOrDomain() instanceof Project project) return project;
		if (scenario != null && scenario.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}
}
