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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchActorException;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editUseCaseCommand")
@Scope("prototype")
public class EditUseCaseCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditUseCaseCommand {

	private UseCase usecase;
	private String primaryActorName;
	private String text;
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
	public EditUseCaseCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public UseCase getUseCase() {
		return usecase;
	}

	public void setUseCase(UseCase usecase) {
		this.usecase = usecase;
	}

	public void setPrimaryActorName(String primaryActorName) {
		this.primaryActorName = primaryActorName;
	}

	protected String getPrimaryActorName() {
		return primaryActorName;
	}

	protected String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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
		ProjectOrDomain projectOrDomain = getRepository().get(getProjectOrDomain());
		User editedBy = getRepository().get(getEditedBy());
		UseCaseImpl usecaseImpl = (UseCaseImpl) getUseCase();

		// check for uniqueness
		try {
			UseCase existing = getProjectRepository().findUseCaseByProjectOrDomainAndName(
					projectOrDomain, getName());
			if (usecaseImpl == null) {
				throw EntityException.uniquenessConflict(UseCase.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(usecaseImpl)) {
				throw EntityException.uniquenessConflict(UseCase.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		Actor primaryActor;
		try {
			primaryActor = getProjectRepository().findActorByProjectOrDomainAndName(
					projectOrDomain, getPrimaryActorName());
		} catch (NoSuchActorException e) {
			EditActorCommand editActorCommand = getProjectCommandFactory().newEditActorCommand();
			editActorCommand.setName(getPrimaryActorName());
			editActorCommand.setActorContainer(projectOrDomain);
			editActorCommand.setEditedBy(editedBy);
			editActorCommand.setProjectOrDomain(projectOrDomain);
			// don't analyze the actor because it only has a name at this point.
			editActorCommand.setAnalysisEnabled(false);
			primaryActor = getCommandHandler().execute(editActorCommand).getActor();
		}

		if (usecaseImpl == null) {
			EditScenarioCommand editScenarioCommand = getProjectCommandFactory()
					.newEditScenarioCommand();
			editScenarioCommand.setEditedBy(editedBy);
			editScenarioCommand.setName(getName());
			editScenarioCommand.setProjectOrDomain(projectOrDomain);
			editScenarioCommand.setScenarioTypeName(ScenarioType.Primary.name());
			editScenarioCommand.setStepCommands(getStepCommands());
			// the scenario will be analyzed when the use case is analyzed.
			editScenarioCommand.setAnalysisEnabled(false);
			editScenarioCommand = getCommandHandler().execute(editScenarioCommand);
			usecaseImpl = getProjectRepository().persist(
					new UseCaseImpl(projectOrDomain, primaryActor, editedBy, getName(), getText(),
							editScenarioCommand.getScenario()));
		} else {
			if (getName() != null) {
				usecaseImpl.setName(getName());
			}
			if (getText() != null) {
				usecaseImpl.setText(getText());
			}
			usecaseImpl = getProjectRepository().merge(usecaseImpl);
			Actor existingPrimaryActor = getRepository().get(usecaseImpl.getPrimaryActor());
			if (primaryActor != null) {
				if ((existingPrimaryActor != null) && !primaryActor.equals(existingPrimaryActor)) {
					existingPrimaryActor.getReferers().remove(usecaseImpl);
				}
				usecaseImpl.setPrimaryActor(primaryActor);
			}
			EditScenarioCommand editScenarioCommand = getProjectCommandFactory()
					.newEditScenarioCommand();
			editScenarioCommand.setScenario(getRepository().get(usecaseImpl.getScenario()));
			editScenarioCommand.setEditedBy(editedBy);
			editScenarioCommand.setName(getName());
			editScenarioCommand.setProjectOrDomain(projectOrDomain);
			editScenarioCommand.setScenarioTypeName(ScenarioType.Primary.name());
			editScenarioCommand.setStepCommands(getStepCommands());
			// the scenario will be analyzed when the use case is analyzed.
			editScenarioCommand.setAnalysisEnabled(false);
			editScenarioCommand = getCommandHandler().execute(editScenarioCommand);
		}
		if (projectOrDomain != null) {
			projectOrDomain.getUseCases().add(usecaseImpl);
		}
		if (primaryActor != null) {
			primaryActor.getReferers().add(usecaseImpl);
		}
		setUseCase(usecaseImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			getAssistantManager().analyzeUseCase(getUseCase());
		}
	}
}
