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
package com.rreganjr.requel.annotation.impl.command;

import com.rreganjr.repository.AbstractRepositoryCommand;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.user.EditCommand;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
public abstract class AbstractEditCommand extends AbstractRepositoryCommand implements EditCommand {

	private final CommandHandler commandHandler;
	private final AnnotationCommandFactory annotationCommandFactory;
	private User editedBy;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	public AbstractEditCommand(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(repository);
		this.commandHandler = commandHandler;
		this.annotationCommandFactory = annotationCommandFactory;

	}

	/**
	 * @see com.rreganjr.requel.command.EditCommand#setEditedBy(com.rreganjr.requel.user.User)
	 */
	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	protected User getEditedBy() {
		return editedBy;
	}

	protected AnnotationRepository getAnnotationRepository() {
		return (AnnotationRepository) getRepository();
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}
}
