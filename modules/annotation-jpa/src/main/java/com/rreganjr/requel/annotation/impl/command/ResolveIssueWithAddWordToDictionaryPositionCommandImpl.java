/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.command.EditProjectDictionaryWordCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;

/**
 * This resolves an issue with the specified position by adding a word to the
 * dictionary.
 * 
 * @author ron
 */
@Controller("resolveIssueWithAddWordToDictionaryPositionCommand")
@Scope("prototype")
public class ResolveIssueWithAddWordToDictionaryPositionCommandImpl extends ResolveIssueCommandImpl {

	private final ProjectCommandFactory projectCommandFactory;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 * @param projectCommandFactory
	 */
	@Autowired
	public ResolveIssueWithAddWordToDictionaryPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository,
			ProjectCommandFactory projectCommandFactory) {
		super(commandHandler, annotationCommandFactory, repository);
		this.projectCommandFactory = projectCommandFactory;
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
		EditProjectDictionaryWordCommand command = projectCommandFactory
				.newEditDictionaryWordCommand();
		command.setLemma(getIssue().getWord());
		// Issue #313: the word belongs to this project's dictionary, not the installation's.
		// getProject() comes from ResolveIssueCommandImpl (ProjectScopedCommand, #305) and resolves
		// the project from the issue's grouping object, so there is nothing extra to resolve here.
		// Issue #312: the project instance is handed over, not its id — the nested command
		// authorizes against it, and a project re-loaded by id has a detached stakeholder
		// collection by the time the check walks it.
		command.setProject(getProject());
		// Issue #312: the nested command is authorized in its own right, and the check reads its
		// subject from getEditedBy(). CurrentUserCommandHandler would inject it over HTTP, but an
		// in-process caller has no SecurityContext and a null editedBy skips the check entirely,
		// so it is set here rather than left to the chain.
		command.setEditedBy(getEditedBy());
		// Issue #312: no try/catch. The write happens before super.execute(), so a failed or
		// refused write propagates and the issue stays unresolved.
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