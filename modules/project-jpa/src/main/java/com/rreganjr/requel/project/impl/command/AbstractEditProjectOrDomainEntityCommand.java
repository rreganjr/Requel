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
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditProjectOrDomainEntityCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
public abstract class AbstractEditProjectOrDomainEntityCommand extends AbstractEditProjectCommand
		implements EditProjectOrDomainEntityCommand {

	private ProjectOrDomain projectOrDomain;
	private boolean analysisEnabled = true;
	private String name;
	private Integer expectedVersion;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	public AbstractEditProjectOrDomainEntityCommand(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setAnalysisEnabled(boolean analysisEnabled) {
		this.analysisEnabled = analysisEnabled;
	}

	protected boolean isAnalysisEnabled() {
		return analysisEnabled;
	}

	/**
	 * @see com.rreganjr.requel.project.command.EditProjectOrDomainEntityCommand#setProjectOrDomain(com.rreganjr.requel.project.ProjectOrDomain)
	 */
	@Override
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain) {
		this.projectOrDomain = projectOrDomain;
	}

	@Override
	public ProjectOrDomain getProjectOrDomain() {
		return projectOrDomain;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	protected String getName() {
		return name;
	}

	@Override
	public void setExpectedVersion(Integer expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	protected Integer getExpectedVersion() {
		return expectedVersion;
	}

	/**
	 * Reload {@code entity} to obtain its authoritative persisted version and enforce
	 * the caller-supplied optimistic-lock version (issue #108). When the caller's
	 * {@link #getExpectedVersion() expected version} is non-null and does not match the
	 * persisted version, the entity changed since the caller loaded it, so this fails
	 * with an {@link EntityLockException} instead of silently overwriting the concurrent
	 * edit. A {@code null} expected version, or a {@code null} entity (a create), skips
	 * the check.
	 *
	 * @param entity the entity being updated (typically just resolved by the registrar)
	 * @return the reloaded, persistence-attached instance for the caller to mutate
	 * @throws EntityLockException if the expected version does not match the persisted one
	 */
	protected <T extends ProjectOrDomainEntity> T checkExpectedVersion(T entity) {
		if (entity == null) {
			return entity;
		}
		T current = getProjectRepository().get(entity);
		if (getExpectedVersion() != null && current != null
				&& getExpectedVersion().intValue() != current.getVersion()) {
			throw EntityLockException.staleEntity(current.getClass(), current,
					EntityExceptionActionType.Updating);
		}
		return current;
	}
}
