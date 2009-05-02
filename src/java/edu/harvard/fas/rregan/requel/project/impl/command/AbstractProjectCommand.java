/*
 * $Id: AbstractProjectCommand.java,v 1.7 2009/03/30 11:54:30 rregan Exp $
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

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.project.impl.assistant.AssistantFacade;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.impl.command.AbstractUserCommand;

/**
 * @author ron
 */
public abstract class AbstractProjectCommand extends AbstractUserCommand {

	private final CommandHandler commandHandler;
	private final ProjectCommandFactory projectCommandFactory;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectRepository projectRepository;
	private final AssistantFacade assistantManager;

	protected AbstractProjectCommand(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(userRepository);
		this.assistantManager = assistantManager;
		this.projectRepository = projectRepository;
		this.projectCommandFactory = projectCommandFactory;
		this.annotationCommandFactory = annotationCommandFactory;
		this.commandHandler = commandHandler;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	protected AssistantFacade getAssistantManager() {
		return assistantManager;
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}
}
