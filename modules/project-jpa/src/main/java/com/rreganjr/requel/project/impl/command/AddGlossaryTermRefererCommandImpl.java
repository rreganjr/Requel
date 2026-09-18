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
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.AddGlossaryTermRefererCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * @see AddGlossaryTermRefererCommand
 *
 * @author ron
 */
@Controller("addGlossaryTermRefererCommand")
@Scope("prototype")
public class AddGlossaryTermRefererCommandImpl extends AbstractEditProjectCommand implements
		AddGlossaryTermRefererCommand, AuthorizableCommand, ProjectScopedCommand {

	private GlossaryTerm glossaryTerm;

	private ProjectOrDomainEntity referer;

	@Autowired
	public AddGlossaryTermRefererCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public GlossaryTerm getGlossaryTerm() {
		return glossaryTerm;
	}

	@Override
	public void setGlossaryTerm(GlossaryTerm glossaryTerm) {
		this.glossaryTerm = glossaryTerm;
	}

	protected ProjectOrDomainEntity getReferer() {
		return referer;
	}

	@Override
	public void setReferer(ProjectOrDomainEntity referer) {
		this.referer = referer;
	}

	/**
	 * Both sides of the association are set, the way
	 * {@code EditGlossaryTermCommandImpl}'s addReferers branch does. The collections are Sets,
	 * so re-running an analysis pass that finds the same phrase again is a no-op rather than a
	 * duplicate.
	 */
	@Override
	public void execute() {
		validate();
		GlossaryTerm term = getProjectRepository().get(getGlossaryTerm());
		ProjectOrDomainEntity entity = getProjectRepository().get(getReferer());
		term.getReferers().add(entity);
		entity.getGlossaryTerms().add(term);
		setGlossaryTerm(getProjectRepository().merge(term));
	}

	protected void validate() {
		if (getGlossaryTerm() == null) {
			throw EntityValidationException.emptyRequiredProperty(GlossaryTerm.class, null,
					"glossaryTerm", EntityExceptionActionType.Updating);
		}
		if (getReferer() == null) {
			throw EntityValidationException.emptyRequiredProperty(GlossaryTerm.class,
					getGlossaryTerm(), "referer", EntityExceptionActionType.Updating);
		}
	}

	@Override
	public Project getProject() {
		if (glossaryTerm != null && glossaryTerm.getProjectOrDomain() instanceof Project project) {
			return project;
		}
		if (referer != null && referer.getProjectOrDomain() instanceof Project project) {
			return project;
		}
		return null;
	}

	/**
	 * A back-reference recorded by an analysis pass is annotation work, not a glossary edit -
	 * see the interface javadoc and issue #302.
	 */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Annotation.class, "Edit");
	}
}
