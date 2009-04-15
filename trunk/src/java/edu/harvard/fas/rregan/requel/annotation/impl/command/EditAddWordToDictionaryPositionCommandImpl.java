/*
 * $Id: EditAddWordToDictionaryPositionCommandImpl.java,v 1.12 2009/02/16 10:10:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Controller("editAddWordToDictionaryPositionCommand")
@Scope("prototype")
public class EditAddWordToDictionaryPositionCommandImpl extends EditPositionCommandImpl implements
		EditAddWordToDictionaryPositionCommand {

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditAddWordToDictionaryPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		LexicalIssue issue = (LexicalIssue) getRepository().get(getIssue());
		AddWordToDictionaryPosition position = getPosition();
		if (position == null) {
			try {
				// search for an existing issue by word
				position = getAnnotationRepository().findAddWordToDictionaryPosition(
						issue.getGroupingObject(), issue.getWord());
			} catch (NoSuchPositionException e) {
				position = getRepository().persist(
						new AddWordToDictionaryPosition(getText(), editedBy));
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
		// is a
		// proxy it will be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	@Override
	public AddWordToDictionaryPosition getPosition() {
		return (AddWordToDictionaryPosition) super.getPosition();
	}

	public void setPosition(AddWordToDictionaryPosition position) {
		super.setPosition(position);
	}
}
