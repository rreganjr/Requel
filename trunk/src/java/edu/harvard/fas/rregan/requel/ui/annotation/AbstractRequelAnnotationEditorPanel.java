/*
 * $Id: AbstractRequelAnnotationEditorPanel.java,v 1.4 2009/02/21 10:32:14 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.annotation;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelEditorPanel;
import edu.harvard.fas.rregan.requel.user.User;

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
		Stakeholder stakeholder = getUserStakeholder();
		if (stakeholder != null) {
			return !stakeholder.hasPermission(Annotation.class, StakeholderPermissionType.Edit);
		}
		return projectEntity;
	}

	@Override
	protected boolean isShowDelete() {
		Stakeholder stakeholder = getUserStakeholder();
		if (stakeholder != null) {
			return !stakeholder.hasPermission(Annotation.class, StakeholderPermissionType.Delete);
		}
		return true;
	}

	protected Stakeholder getUserStakeholder() {
		User user = (User) getApp().getUser();
		Annotatable annotatable = getAnnotatable();
		if (annotatable != null) {
			Stakeholder stakeholder = null;
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
