/*
 * $Id: AbstractRequelProjectEditorPanel.java,v 1.3 2009/02/21 10:32:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelEditorPanel;
import edu.harvard.fas.rregan.requel.user.User;

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
		Stakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return !stakeholder.hasPermission(getSupportedContentType(),
					StakeholderPermissionType.Edit);
		}
		return true;
	}

	@Override
	protected boolean isShowDelete() {
		Stakeholder stakeholder = getUserStakeholder(getTargetObject());
		if (stakeholder != null) {
			return stakeholder.hasPermission(getSupportedContentType(),
					StakeholderPermissionType.Delete);
		}
		return false;
	}

	protected Stakeholder getUserStakeholder(Object target) {
		User user = (User) getApp().getUser();
		Project project = null;
		Stakeholder stakeholder = null;
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
