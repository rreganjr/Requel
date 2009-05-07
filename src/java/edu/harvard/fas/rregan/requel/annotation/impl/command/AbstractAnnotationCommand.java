/*
 * $Id: AbstractAnnotationCommand.java,v 1.5 2009/02/13 12:07:59 rregan Exp $
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