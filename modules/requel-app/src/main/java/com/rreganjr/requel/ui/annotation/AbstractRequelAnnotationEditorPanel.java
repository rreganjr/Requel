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
package com.rreganjr.requel.ui.annotation;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.ui.AbstractRequelEditorPanel;
import com.rreganjr.requel.user.User;

/**
 * Base class for Annotation editors with access to an annotation repository.
 * 
 * @author ron
 */
public abstract class AbstractRequelAnnotationEditorPanel extends AbstractRequelEditorPanel {

	private final AnnotationRepository annotationRepository;

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param commandHandler
	 * @param annotationRepository
	 */
	public AbstractRequelAnnotationEditorPanel(String resourceBundleName,
			Class<?> supportedContentType, CommandHandler commandHandler,
			AnnotationRepository annotationRepository) {
		super(resourceBundleName, supportedContentType, commandHandler);
		this.annotationRepository = annotationRepository;
	}

	/**
	 * @param resourceBundleName
	 * @param supportedContentType
	 * @param panelName
	 * @param commandHandler
	 * @param annotationRepository
	 */
	public AbstractRequelAnnotationEditorPanel(String resourceBundleName,
			Class<?> supportedContentType, String panelName, CommandHandler commandHandler,
			AnnotationRepository annotationRepository) {
		super(resourceBundleName, supportedContentType, panelName, commandHandler);
		this.annotationRepository = annotationRepository;
	}

	protected AnnotationRepository getAnnotationRepository() {
		return annotationRepository;
	}

	protected Annotatable getAnnotatable() {
		if (getTargetObject() instanceof Annotatable) {
			return (Annotatable) getTargetObject();
		}
		return null;
	}

	protected Object getGroupingObject() {
		// TODO: project classes are leaking into non-project package
		if (getAnnotatable() instanceof ProjectOrDomainEntity) {
			ProjectOrDomainEntity entity = (ProjectOrDomainEntity) getAnnotatable();
			return entity.getProjectOrDomain();
		} else if (getAnnotatable() instanceof ProjectOrDomain) {
			return getAnnotatable();
		} else if (getAnnotatable() instanceof GoalRelation) {
			GoalRelation entity = (GoalRelation) getAnnotatable();
			return entity.getFromGoal().getProjectOrDomain();
		}
		return null;
	}

	@Override
	public boolean isReadOnlyMode() {
		boolean projectEntity = isProjectEntity(getAnnotatable());
		Stakeholder stakeholder = getStakeholder();
		if ((stakeholder != null) && stakeholder.isUserStakeholder()) {
			return !((UserStakeholder) stakeholder).hasPermission(Annotation.class,
					StakeholderPermissionType.Edit);
		}
		return projectEntity;
	}

	@Override
	protected boolean isShowDelete() {
		Stakeholder stakeholder = getStakeholder();
		if ((stakeholder != null) && stakeholder.isUserStakeholder()) {
			return !((UserStakeholder) stakeholder).hasPermission(Annotation.class,
					StakeholderPermissionType.Delete);
		}
		return true;
	}

	protected UserStakeholder getStakeholder() {
		User user = (User) getApp().getUser();
		Annotatable annotatable = getAnnotatable();
		if (annotatable != null) {
			UserStakeholder stakeholder = null;
			if (annotatable instanceof Project) {
				Project project = (Project) annotatable;
				stakeholder = project.getUserStakeholder(user);
			} else if (annotatable instanceof ProjectOrDomainEntity) {
				ProjectOrDomainEntity podEntity = (ProjectOrDomainEntity) annotatable;
				if (podEntity.getProjectOrDomain() instanceof Project) {
					Project project = (Project) podEntity.getProjectOrDomain();
					stakeholder = project.getUserStakeholder(user);
				}
			}
			if (stakeholder != null) {
				return stakeholder;
			}
		}
		return null;
	}

	protected boolean isProjectEntity(Annotatable annotatable) {
		boolean projectEntity = false;
		if (annotatable != null) {
			if ((annotatable instanceof Project) || (annotatable instanceof ProjectOrDomainEntity)) {
				projectEntity = true;
			}
		}
		return projectEntity;
	}
}
