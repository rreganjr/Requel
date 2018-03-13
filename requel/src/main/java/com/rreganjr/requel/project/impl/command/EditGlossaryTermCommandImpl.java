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
package com.rreganjr.requel.project.impl.command;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.EntityValidationException;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
@Controller("editGlossaryTermCommand")
@Scope("prototype")
public class EditGlossaryTermCommandImpl extends AbstractEditProjectOrDomainEntityCommand implements
		EditGlossaryTermCommand {

	private Set<ProjectOrDomainEntity> referers;
	private Set<ProjectOrDomainEntity> addReferers;
	private GlossaryTerm glossaryTerm;
	private GlossaryTerm canonicalTerm;
	private String definition;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditGlossaryTermCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setReferers(Set<ProjectOrDomainEntity> referers) {
		this.referers = referers;
	}

	protected Set<ProjectOrDomainEntity> getReferers() {
		return referers;
	}

	@Override
	public void setAddReferers(Set<ProjectOrDomainEntity> addReferers) {
		this.addReferers = addReferers;
	}

	protected Set<ProjectOrDomainEntity> getAddReferers() {
		return addReferers;
	}

	@Override
	public GlossaryTerm getGlossaryTerm() {
		return glossaryTerm;
	}

	@Override
	public void setGlossaryTerm(GlossaryTerm glossaryTerm) {
		this.glossaryTerm = glossaryTerm;
	}

	/**
	 * @return the primary/prefered term the term being edited is an alternative
	 *         to.
	 */
	public GlossaryTerm getCanonicalTerm() {
		return canonicalTerm;
	}

	@Override
	public void setCanonicalTerm(GlossaryTerm canonicalTerm) {
		this.canonicalTerm = canonicalTerm;
	}

	/**
	 * @return the definition of the term.
	 */
	public String getDefinition() {
		return definition;
	}

	@Override
	public void setText(String definition) {
		this.definition = definition;
	}

	@Override
	public void execute() {
		ProjectOrDomain projectOrDomain = getProjectRepository().get(getProjectOrDomain());
		GlossaryTerm canonicalTerm = getProjectRepository().get(getCanonicalTerm());
		User editedBy = getProjectRepository().get(getEditedBy());
		GlossaryTermImpl glossaryTermImpl = (GlossaryTermImpl) getGlossaryTerm();
		// check for uniqueness
		try {
			String name = getName();
			if (name == null) {
				if (glossaryTermImpl == null) {
					throw EntityValidationException.emptyRequiredProperty(GlossaryTerm.class, null,
							"name", EntityExceptionActionType.Updating);
				}
				name = glossaryTermImpl.getName();
			}
			GlossaryTerm existing = getProjectRepository().findGlossaryTermForProjectOrDomain(
					projectOrDomain, name);
			if (glossaryTermImpl == null) {
				throw EntityException.uniquenessConflict(GlossaryTerm.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(glossaryTermImpl)) {
				throw EntityException.uniquenessConflict(GlossaryTerm.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}

		if (glossaryTermImpl == null) {
			glossaryTermImpl = getProjectRepository().persist(
					new GlossaryTermImpl(projectOrDomain, getName(), editedBy));
			projectOrDomain.getGlossaryTerms().add(glossaryTermImpl);
		} else if (getName() != null) {
			glossaryTermImpl.setName(getName());
		}
		if (getDefinition() != null) {
			glossaryTermImpl.setText(getDefinition());
		}
		if (canonicalTerm != null) {
			glossaryTermImpl.setCanonicalTerm(canonicalTerm);
		}

		glossaryTermImpl = getProjectRepository().merge(glossaryTermImpl);

		if (getReferers() != null) {
			// remove the term for all its referers
			for (ProjectOrDomainEntity entity : glossaryTermImpl.getReferers()) {
				if (entity != null) {
					entity.getGlossaryTerms().remove(glossaryTermImpl);
				}
			}
			// add the referers to the term and the term to the referers
			glossaryTermImpl.getReferers().clear();
			for (ProjectOrDomainEntity entity : getReferers()) {
				entity = getProjectRepository().get(entity);
				glossaryTermImpl.getReferers().add(entity);
				entity.getGlossaryTerms().add(glossaryTermImpl);
			}
		} else if (getAddReferers() != null) {
			for (ProjectOrDomainEntity entity : getAddReferers()) {
				entity = getProjectRepository().get(entity);
				glossaryTermImpl.getReferers().add(entity);
				entity.getGlossaryTerms().add(glossaryTermImpl);
			}
		}
		if (canonicalTerm != null) {
			canonicalTerm.getAlternateTerms().add(glossaryTermImpl);
		}
		setCanonicalTerm(canonicalTerm);
		setGlossaryTerm(glossaryTermImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: analyze the glossary term?
		}
	}
}
