/*
 * $Id: AbstractAnnotationCommand.java,v 1.5 2009/02/13 12:07:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditAnnotationCommand;

/**
 * @author ron
 */
public abstract class AbstractAnnotationCommand extends AbstractEditCommand implements
		EditAnnotationCommand {

	private Object groupingObject;
	private Annotatable annotatable;
	private String text;

	protected AbstractAnnotationCommand(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	protected Object getGroupingObject() {
		return groupingObject;
	}

	public void setGroupingObject(Object groupingObject) {
		this.groupingObject = groupingObject;
	}

	protected Annotatable getAnnotatable() {
		return annotatable;
	}

	public void setAnnotatable(Annotatable annotatable) {
		this.annotatable = annotatable;
	}

	protected String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
