/*
 * $Id: ResolveIssueWithAddWordToDictionaryPositionCommandImpl.java,v 1.7 2009/02/17 11:50:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditDictionaryWordCommand;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.impl.AddWordToDictionaryPosition;
import edu.harvard.fas.rregan.requel.annotation.impl.LexicalIssue;

/**
 * This resolves an issue with the specified position by adding a word to the
 * dictionary.
 * 
 * @author ron
 */
@Controller("resolveIssueWithAddWordToDictionaryPositionCommand")
@Scope("prototype")
public class ResolveIssueWithAddWordToDictionaryPositionCommandImpl extends ResolveIssueCommandImpl {

	private final DictionaryCommandFactory dictionaryCommandFactory;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 * @param dictionaryCommandFactory
	 */
	@Autowired
	public ResolveIssueWithAddWordToDictionaryPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository,
			DictionaryCommandFactory dictionaryCommandFactory) {
		super(commandHandler, annotationCommandFactory, repository);
		this.dictionaryCommandFactory = dictionaryCommandFactory;
	}

	@Override
	public LexicalIssue getIssue() {
		return (LexicalIssue) super.getIssue();
	}

	@Override
	protected AddWordToDictionaryPosition getPosition() {
		return (AddWordToDictionaryPosition) super.getPosition();
	}

	@Override
	public void execute() throws Exception {
		validate();
		EditDictionaryWordCommand command = dictionaryCommandFactory.newEditDictionaryWordCommand();
		command.setLemma(getIssue().getWord());
		command = getCommandHandler().execute(command);
		super.execute();
	}

	@Override
	protected void validate() {
		if (getIssue() == null) {
			throw EntityValidationException.emptyRequiredProperty(LexicalIssue.class, getIssue(),
					"issue", EntityExceptionActionType.Updating);
		}
	}
}