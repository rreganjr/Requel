/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.VersionCheckedDeleteCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * Base for the entity deletes that check the caller's optimistic-lock version (issue #296); see
 * {@link VersionCheckedDeleteCommand}. {@code DeleteProjectCommandImpl} does the same check itself,
 * under the project row lock it takes first.
 *
 * @author ron
 */
public abstract class AbstractDeleteProjectEntityCommand extends AbstractEditProjectCommand
		implements VersionCheckedDeleteCommand {

	private Integer expectedVersion;

	public AbstractDeleteProjectEntityCommand(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setExpectedVersion(Integer expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	protected Integer getExpectedVersion() {
		return expectedVersion;
	}

	/**
	 * Refuse the delete when the caller supplied a version and the entity, just reloaded, is at a
	 * different one. Call it before changing anything.
	 *
	 * @param entityType     the entity's domain type, for the message
	 * @param entity         the reloaded entity
	 * @param currentVersion its persisted version
	 * @throws EntityLockException if the caller's version is stale
	 */
	protected void refuseStaleDelete(Class<?> entityType, Object entity, int currentVersion) {
		if (expectedVersion != null && expectedVersion.intValue() != currentVersion) {
			throw EntityLockException.staleEntity(entityType, entity,
					EntityExceptionActionType.Deleting);
		}
	}
}
