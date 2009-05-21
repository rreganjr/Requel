/*
 * $Id$
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.ArgumentPositionSupportLevel;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.ArgumentImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Controller("editArgumentCommand")
@Scope("prototype")
public class EditArgumentCommandImpl extends AbstractEditCommand implements EditArgumentCommand {

	private Position position;
	private Argument argument;
	private String text;
	private String supportLevelName;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditArgumentCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand#getArgument()
	 */
	@Override
	public Argument getArgument() {
		return argument;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand#setArgument(edu.harvard.fas.rregan.requel.annotation.Argument)
	 */
	@Override
	public void setArgument(Argument argument) {
		this.argument = argument;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand#setPosition(edu.harvard.fas.rregan.requel.annotation.Position)
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	protected Position getPosition() {
		return position;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand#setSupportLevelName(java.lang.String)
	 */
	@Override
	public void setSupportLevelName(String supportLevelName) {
		this.supportLevelName = supportLevelName;
	}

	protected String getSupportLevelName() {
		return supportLevelName;
	}

	protected ArgumentPositionSupportLevel getSupportLevel() {
		return ArgumentPositionSupportLevel.valueOf(getSupportLevelName());
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditArgumentCommand#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Position position = getRepository().get(getPosition());
		ArgumentImpl argumentImpl = (ArgumentImpl) getArgument();
		if (argumentImpl == null) {
			argumentImpl = getRepository().persist(
					new ArgumentImpl(position, getText(), getSupportLevel(), editedBy));
		} else {
			argumentImpl.setText(getText());
			argumentImpl.setSupportLevel(getSupportLevel());
			argumentImpl = getRepository().merge(argumentImpl);
		}
		setArgument(argumentImpl);
		if (position != null) {
			position.getArguments().add(argumentImpl);
			setPosition(position);
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Argument.class, getArgument(),
					"text", EntityExceptionActionType.Updating);
		}
		if ((getSupportLevelName() == null) || "".equals(getSupportLevelName().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Argument.class, getArgument(),
					"supportLevel", EntityExceptionActionType.Updating);
		}
		try {
			if (getSupportLevel() == null) {
				throw EntityValidationException.validationFailed(Argument.class, "supportLevel",
						"The specified support level \"" + getSupportLevelName()
								+ "\" is not valid.");
			}
		} catch (Exception e) {
			throw EntityValidationException.validationFailed(Argument.class, "supportLevel",
					"The specified support level \"" + getSupportLevelName() + "\" is not valid.");
		}
	}
}
