/*
 * $Id: EditStakeholderCommandImpl.java 124 2009-05-21 23:46:02Z rreganjr $
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.NoSuchEntityException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Create or edit a non-user stakeholder.
 * 
 * @author ron
 */
@Controller("editNonUserStakeholderCommand")
@Scope("prototype")
public class EditNonUserStakeholderCommandImpl extends AbstractEditProjectOrDomainEntityCommand
		implements EditNonUserStakeholderCommand {

	private NonUserStakeholder stakeholder;
	private String text;
	
	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public EditNonUserStakeholderCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	public NonUserStakeholder getStakeholder() {
		return stakeholder;
	}

	public void setStakeholder(NonUserStakeholder stakeholder) {
		this.stakeholder = stakeholder;
	}

	
	protected String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void execute() {
		ProjectOrDomain projectOrDomain = getProjectRepository().get(getProjectOrDomain());
		User editedBy = getProjectRepository().get(getEditedBy());

		NonUserStakeholderImpl stakeholderImpl = (NonUserStakeholderImpl) getStakeholder();

		// check for uniqueness
		try {
			NonUserStakeholder existing = getProjectRepository()
					.findStakeholderByProjectOrDomainAndName(projectOrDomain, getName());
			if (stakeholderImpl == null) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing, FIELD_NAME,
						EntityExceptionActionType.Creating);
			} else if (!existing.equals(stakeholderImpl)) {
				throw EntityException.uniquenessConflict(Stakeholder.class, existing, FIELD_NAME,
						EntityExceptionActionType.Updating);
			}
		} catch (NoSuchEntityException e) {
		}


		if (stakeholderImpl == null) {
			stakeholderImpl = getProjectRepository().persist(
					new NonUserStakeholderImpl(projectOrDomain, editedBy, getName()));
		} else {
			stakeholderImpl.setName(getName());
		}
		stakeholderImpl.setText(getText());
		stakeholderImpl = getProjectRepository().merge(stakeholderImpl);
		setStakeholder(stakeholderImpl);
	}

	@Override
	public void invokeAnalysis() {
		if (isAnalysisEnabled()) {
			// TODO: analyze stakeholder?
		}
	}
}
