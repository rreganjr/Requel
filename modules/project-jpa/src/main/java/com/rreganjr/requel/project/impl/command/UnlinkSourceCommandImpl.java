/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.UnlinkSourceCommand;
import com.rreganjr.requel.project.impl.ProvenanceEntityTypes;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #272: see {@link UnlinkSourceCommand}. The source itself is kept, even with no links left:
 * a source with no derived entities is still a record of the artifact (#273 cites them).
 */
@Controller("unlinkSourceCommand")
@Scope("prototype")
public class UnlinkSourceCommandImpl extends AbstractProjectCommand
		implements UnlinkSourceCommand, AuthorizableCommand {

	private Project project;
	private ProjectOrDomainEntity target;
	private String system;
	private String externalId;
	private String fragment;
	private User editedBy;
	private boolean unlinked;

	@Autowired
	public UnlinkSourceCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
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
	public void setTarget(ProjectOrDomainEntity target) {
		this.target = target;
	}

	@Override
	public void setSystem(String system) {
		this.system = system;
	}

	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@Override
	public void setFragment(String fragment) {
		this.fragment = fragment;
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
	public boolean isUnlinked() {
		return unlinked;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		Class<?> type = target == null ? ProjectOrDomainEntity.class
				: target.getProjectOrDomainEntityInterface();
		return new RequiresStakeholderPermission(ProvenanceEntityTypes.permissionType(type), "Edit");
	}

	@Override
	public void execute() throws Exception {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Deleting);
		}
		if (target == null) {
			throw EntityValidationException.emptyRequiredProperty(ProjectOrDomainEntity.class, null,
					"entity", EntityExceptionActionType.Deleting);
		}
		ProvenanceStore store = RecordSourceCommandImpl.requireProvenanceStore(getProvenanceStore());
		ExternalSource source = RecordSourceCommandImpl.requireSource(store, project, system,
				externalId);
		unlinked = store.unlink(source.getId(), SourceLinkRelation.DERIVED_FROM,
				ProvenanceEntityTypes.nameOf(target), target.getId(), fragment);
	}
}
