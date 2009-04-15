/*
 * $Id: EditChangeSpellingPositionCommandImpl.java,v 1.13 2009/02/16 10:10:08 rregan Exp $
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
import edu.harvard.fas.rregan.requel.annotation.command.EditChangeSpellingPositionCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.ChangeSpellingPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Create or edit a position for changing the spelling of a word in an entity.
 * 
 * @author ron
 */
@Controller("editChangeSpellingPositionCommand")
@Scope("prototype")
public class EditChangeSpellingPositionCommandImpl extends EditPositionCommandImpl implements
		EditChangeSpellingPositionCommand {

	private String proposedWord;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditChangeSpellingPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	protected String getProposedWord() {
		return proposedWord;
	}

	public void setProposedWord(String proposedWord) {
		this.proposedWord = proposedWord;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		LexicalIssue issue = (LexicalIssue) getRepository().get(getIssue());
		ChangeSpellingPosition position = getPosition();
		if (position == null) {
			try {
				// search for an existing position
				position = getAnnotationRepository().findChangeSpellingPosition(issue,
						getProposedWord());
			} catch (NoSuchPositionException e) {
				position = getRepository().persist(
						new ChangeSpellingPosition(getText(), editedBy, getProposedWord()));
			}
		} else {
			position.setText(getText());
			position.setProposedWord(getProposedWord());
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

	@Override
	public ChangeSpellingPosition getPosition() {
		return (ChangeSpellingPosition) super.getPosition();
	}

	/**
	 * @param position
	 */
	public void setPosition(ChangeSpellingPosition position) {
		super.setPosition(position);
	}

}
