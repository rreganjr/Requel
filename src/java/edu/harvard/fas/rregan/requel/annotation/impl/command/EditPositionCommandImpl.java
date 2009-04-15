/*
 * $Id: EditPositionCommandImpl.java,v 1.14 2009/02/17 11:50:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.user.User;

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
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand#setIssue(edu.harvard.fas.rregan.requel.annotation.Issue)
	 */
	@Override
	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected Issue getIssue() {
		return issue;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand#getPosition()
	 */
	@Override
	public Position getPosition() {
		return position;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand#setPosition(edu.harvard.fas.rregan.requel.annotation.Position)
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.EditPositionCommand#setText(java.lang.String)
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
