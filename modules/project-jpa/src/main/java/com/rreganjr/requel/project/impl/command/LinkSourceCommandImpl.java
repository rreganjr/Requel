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
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.SourceLinks;
import com.rreganjr.requel.project.command.LinkSourceCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProvenanceEntityTypes;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #272: see {@link LinkSourceCommand}. The link records the entity as it is now and, when
 * given, the fragment's text, so a later {@code UpsertFromSource} can tell change on either side.
 */
@Controller("linkSourceCommand")
@Scope("prototype")
public class LinkSourceCommandImpl extends AbstractProjectCommand
		implements LinkSourceCommand, AuthorizableCommand {

	private Project project;
	private ProjectOrDomainEntity target;
	private String system;
	private String externalId;
	private String fragment;
	private String fragmentText;
	private User editedBy;
	private EntitySourceLink link;

	@Autowired
	public LinkSourceCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
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
	public void setFragmentText(String fragmentText) {
		this.fragmentText = fragmentText;
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
	public EntitySourceLink getLink() {
		return link;
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
					EntityExceptionActionType.Updating);
		}
		if (target == null) {
			throw EntityValidationException.emptyRequiredProperty(ProjectOrDomainEntity.class, null,
					"entity", EntityExceptionActionType.Updating);
		}
		ProvenanceStore store = RecordSourceCommandImpl.requireProvenanceStore(getProvenanceStore());
		String typeName = ProvenanceEntityTypes.nameOf(target);
		ProvenanceEntityTypes.require(typeName);
		ExternalSource source = RecordSourceCommandImpl.requireSource(store, project, system,
				externalId);
		String fragmentHash = (fragmentText == null || fragmentText.isBlank()) ? null
				: CriterionHash.of(fragmentText);
		link = store.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
				typeName, target.getId(), fragment, fragmentHash, source.getContentHash(),
				SourceLinks.fingerprint(target)), getRepository().get(editedBy));
	}
}
