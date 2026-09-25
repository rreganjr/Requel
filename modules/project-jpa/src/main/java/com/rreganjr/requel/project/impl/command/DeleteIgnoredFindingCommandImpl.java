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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationExemptable;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.IgnoredFinding;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.AnalysisRequestSource;
import com.rreganjr.requel.project.command.DeleteIgnoredFindingCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.IgnorableEntityTypes;
import com.rreganjr.requel.project.impl.IgnoredFindingImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #320: see {@link DeleteIgnoredFindingCommand}. Removing only the row would change nothing
 * visible, because the resolved issue it came from still matches the finding's key and keeps it
 * resolved; so that issue is unlinked from the entity too. Nothing is reopened.
 */
@Controller("deleteIgnoredFindingCommand")
@Scope("prototype")
public class DeleteIgnoredFindingCommandImpl extends AbstractProjectCommand
		implements DeleteIgnoredFindingCommand, AuthorizableCommand, AnalysisRequestSource {

	private Project project;
	private Long ignoredFindingId;
	private User editedBy;
	private ProjectOrDomainEntity target;

	@Autowired
	public DeleteIgnoredFindingCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setIgnoredFindingId(Long ignoredFindingId) {
		this.ignoredFindingId = ignoredFindingId;
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Project.class, "Edit");
	}

	@Override
	public void execute() throws Exception {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Deleting);
		}
		if (ignoredFindingId == null) {
			throw EntityValidationException.emptyRequiredProperty(IgnoredFindingImpl.class, null,
					"ignoredFindingId", EntityExceptionActionType.Deleting);
		}
		IgnoredFinding ignored = getIgnoredFindingStore().find(project.getId(), ignoredFindingId)
				.orElseThrow(() -> NoSuchEntityException.byQuery(IgnoredFindingImpl.class, "id",
						ignoredFindingId));
		target = loadTarget(ignored);
		getIgnoredFindingStore().delete(project.getId(), ignoredFindingId);
		if (target != null && ignored.getAnnotationId() != null) {
			unlinkResolvedIssue(target, ignored.getAnnotationId());
		}
	}

	private ProjectOrDomainEntity loadTarget(IgnoredFinding ignored) {
		Class<?> type = IgnorableEntityTypes.BY_NAME.get(ignored.getTargetType());
		if (type == null) {
			return null;
		}
		try {
			Object entity = getProjectRepository().findById(type, ignored.getTargetId());
			return (entity instanceof ProjectOrDomainEntity found) ? found : null;
		} catch (NoSuchEntityException e) {
			return null;
		}
	}

	/**
	 * Unlink the issue from this entity only: an issue the pre-#320 leak attached to several
	 * entities stays on the others. RemoveAnnotationFromAnnotatable deletes it once nothing else
	 * has it. It runs auth-exempt as a sub-step of this already-authorized command.
	 */
	private void unlinkResolvedIssue(ProjectOrDomainEntity entity, Long annotationId)
			throws Exception {
		for (Annotation annotation : entity.getAnnotations()) {
			Annotation unproxied = (Annotation) Hibernate.unproxy(annotation);
			if (annotationId.equals(unproxied.getId())) {
				RemoveAnnotationFromAnnotatableCommand command = getAnnotationCommandFactory()
						.newRemoveAnnotationFromAnnotatableCommand();
				command.setAnnotatable(entity);
				command.setAnnotation(unproxied);
				command.setEditedBy(editedBy);
				((AuthorizationExemptable) command).setAuthorizationExempt(true);
				getCommandHandler().execute(command);
				return;
			}
		}
	}

	/** The entity whose ignore was removed: one run raises the finding again. */
	@Override
	public ProjectOrDomainEntity getAnalysisTarget() {
		return target;
	}

	@Override
	public User getAnalysisTriggeredBy() {
		return editedBy;
	}
}
