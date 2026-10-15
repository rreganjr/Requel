/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteArgumentCommand;

/**
 * @author ron
 */
@Controller("deleteArgumentCommand")
@Scope("prototype")
public class DeleteArgumentCommandImpl extends AbstractEditCommand implements DeleteArgumentCommand, com.rreganjr.requel.project.ProjectScopedCommand,
		com.rreganjr.platform.command.AuthorizableCommand {

	private Argument argument;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public DeleteArgumentCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.DeleteArgumentCommand#setArgument(com.rreganjr.requel.annotation.Argument)
	 */
	@Override
	public void setArgument(Argument argument) {
		this.argument = argument;
	}

	protected Argument getArgument() {
		return argument;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Argument argument = getRepository().get(getArgument());
		Position position = getRepository().get(argument.getPosition());
		position.getArguments().remove(argument);
		getRepository().delete(argument);
	}


	@Override
	public com.rreganjr.requel.project.Project getProject() {
		return AnnotationCommandProjectResolver.of(argument);
	}

	@Override
	public com.rreganjr.platform.command.AuthorizationRequirement getAuthorizationRequirement() {
		return new com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission(com.rreganjr.requel.annotation.Annotation.class, "Delete");
	}
}
