/*
 * $Id: DeleteArgumentCommandImpl.java,v 1.1 2009/02/13 12:08:00 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteArgumentCommand;

/**
 * @author ron
 */
@Controller("deleteArgumentCommand")
@Scope("prototype")
public class DeleteArgumentCommandImpl extends AbstractEditCommand implements DeleteArgumentCommand {

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
	 * @see edu.harvard.fas.rregan.requel.annotation.command.DeleteArgumentCommand#setArgument(edu.harvard.fas.rregan.requel.annotation.Argument)
	 */
	@Override
	public void setArgument(Argument argument) {
		this.argument = argument;
	}

	protected Argument getArgument() {
		return argument;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Argument argument = getRepository().get(getArgument());
		Position position = getRepository().get(argument.getPosition());
		position.getArguments().remove(argument);
		getRepository().delete(argument);
	}

}
