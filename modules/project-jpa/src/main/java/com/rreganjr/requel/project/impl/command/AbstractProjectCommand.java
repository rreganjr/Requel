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
package com.rreganjr.requel.project.impl.command;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAllAnnotationsFromAnnotatableCommand;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.command.AbstractUserCommand;
import com.rreganjr.platform.command.AuthorizationExemptable;

/**
 * @author ron
 */
public abstract class AbstractProjectCommand extends AbstractUserCommand
		implements AuthorizationExemptable {

	// TODO(#75): temporary. Lets a parent command mark internally-invoked cascade sub-commands
	// (e.g. detach steps in a delete) exempt from re-authorization, so a Delete-only stakeholder
	// isn't re-checked for Edit on each container. Remove with the permission-coherence model:
	// https://github.com/rreganjr/Requel/issues/75
	private boolean authorizationExempt = false;

	@Override
	public boolean isAuthorizationExempt() {
		return authorizationExempt;
	}

	@Override
	public void setAuthorizationExempt(boolean authorizationExempt) {
		this.authorizationExempt = authorizationExempt;
	}

	private final CommandHandler commandHandler;
	private final ProjectCommandFactory projectCommandFactory;
	private final AnnotationCommandFactory annotationCommandFactory;
	private final ProjectRepository projectRepository;
	private final AssistantFacade assistantManager;

	protected AbstractProjectCommand(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(userRepository);
		this.assistantManager = assistantManager;
		this.projectRepository = projectRepository;
		this.projectCommandFactory = projectCommandFactory;
		this.annotationCommandFactory = annotationCommandFactory;
		this.commandHandler = commandHandler;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	protected AssistantFacade getAssistantManager() {
		return assistantManager;
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	/**
	 * #247: the last step before {@code getRepository().delete(entity)} in every
	 * project-entity delete. Unlinks the entity from every annotation using the
	 * database's current state (index-backed native deletes) and deletes the
	 * annotations left orphaned, so an annotation a background assistant linked to
	 * the entity after this command loaded it cannot fail the delete with a
	 * {@code <table>_annotations} foreign-key violation. Runs auth-exempt as an
	 * intrinsic sub-step of the already-authorized delete (#75).
	 *
	 * @param entity
	 *            the managed entity about to be deleted.
	 * @param editedBy
	 *            the acting user.
	 * @throws Exception
	 */
	protected void removeAllAnnotationsBeforeDelete(Annotatable entity, User editedBy)
			throws Exception {
		RemoveAllAnnotationsFromAnnotatableCommand command = getAnnotationCommandFactory()
				.newRemoveAllAnnotationsFromAnnotatableCommand();
		command.setAnnotatable(entity);
		command.setEditedBy(editedBy);
		((AuthorizationExemptable) command).setAuthorizationExempt(true);
		getCommandHandler().execute(command);
	}
}
