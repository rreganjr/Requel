/*
 * $Id: AbstractEditCommand.java,v 1.1 2009/02/13 12:07:57 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.impl.command;

import edu.harvard.fas.rregan.command.AbstractCommand;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public abstract class AbstractEditCommand extends AbstractCommand implements EditCommand {

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
	 * @see edu.harvard.fas.rregan.requel.command.EditCommand#setEditedBy(edu.harvard.fas.rregan.requel.user.User)
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
