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
package com.rreganjr.requel.ui.project;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.ui.AbstractRequelEditorPanel;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
public class AbstractRequelProjectEditorPanel extends AbstractRequelEditorPanel {
	static final long serialVersionUID = 0L;

	private final ProjectCommandFactory projectCommandFactory;
	private final ProjectRepository projectRepository;

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param panelName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public AbstractRequelProjectEditorPanel(String resourceBundleName,
			Class<?> supportedContentType, String panelName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, supportedContentType, panelName, commandHandler);
		this.projectCommandFactory = projectCommandFactory;
		this.projectRepository = projectRepository;
	}

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public AbstractRequelProjectEditorPanel(String resourceBundleName,
			Class<?> supportedContentType, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, supportedContentType, commandHandler);
		this.projectCommandFactory = projectCommandFactory;
		this.projectRepository = projectRepository;
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	@Override
	public boolean isReadOnlyMode() {
		UserStakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return !stakeholder.hasPermission(getSupportedContentType(),
					StakeholderPermissionType.Edit);
		}
		return true;
	}

	@Override
	protected boolean isShowDelete() {
		UserStakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return stakeholder.hasPermission(getSupportedContentType(),
					StakeholderPermissionType.Delete);
		}
		return false;
	}

	protected UserStakeholder getUserStakeholder(Object target) {
		User user = (User) getApp().getUser();
		Project project = null;
		UserStakeholder stakeholder = null;
		if (target instanceof Project) {
			project = (Project) target;
			stakeholder = project.getUserStakeholder(user);
		} else if (target instanceof ProjectOrDomainEntity) {
			ProjectOrDomainEntity podEntity = (ProjectOrDomainEntity) target;
			if (podEntity.getProjectOrDomain() instanceof Project) {
				project = (Project) podEntity.getProjectOrDomain();
				stakeholder = project.getUserStakeholder(user);
			}
		}
		if (stakeholder != null) {
			return stakeholder;
		}
		return null;
	}
}
