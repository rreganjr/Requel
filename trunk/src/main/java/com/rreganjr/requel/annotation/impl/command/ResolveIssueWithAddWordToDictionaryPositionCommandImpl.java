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
import com.rreganjr.nlp.dictionary.command.DictionaryCommandFactory;
import com.rreganjr.nlp.dictionary.command.EditDictionaryWordCommand;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.EntityValidationException;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;

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