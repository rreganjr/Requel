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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.EntityValidationException;
import com.rreganjr.NoSuchEntityException;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
@Controller("editPositionCommand")
@Scope("prototype")
public class EditPositionCommandImpl extends AbstractEditCommand implements EditPositionCommand {

	private Position position;
	private Issue issue;
	private String text;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setIssue(com.rreganjr.requel.annotation.Issue)
	 */
	@Override
	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected Issue getIssue() {
		return issue;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#getPosition()
	 */
	@Override
	public Position getPosition() {
		return position;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setPosition(com.rreganjr.requel.annotation.Position)
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		PositionImpl position = (PositionImpl) getPosition();
		if (position == null) {
			// look for existing position that matches the text and
			// reference it with the issue.
			if (issue != null) {
				try {
					getAnnotationRepository().findPosition(issue.getGroupingObject(), getText());
				} catch (NoSuchEntityException e) {
					position = getRepository().persist(new PositionImpl(getText(), editedBy));
				}
			} else {
				position = getRepository().persist(new PositionImpl(getText(), editedBy));
			}
		} else {
			position.setText(getText());
			position = getRepository().merge(position);
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		setPosition(position);

		// add the position to the issue after it has been merged so that if it
		// is a proxy it will be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Position.class, getPosition(),
					"text", EntityExceptionActionType.Updating);
		}
	}
}
