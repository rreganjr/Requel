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

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;
import com.rreganjr.nlp.dictionary.impl.command.AbstractDictionaryCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteProjectDictionaryWordCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * Removes one word from a project's dictionary (issue #319). See
 * {@link AddProjectDictionaryWordCommandImpl} for why it extends
 * {@link AbstractDictionaryCommand} and holds the resolved {@link Project}.
 *
 * @author ron
 */
@Controller("deleteProjectDictionaryWordCommand")
@Scope("prototype")
public class DeleteProjectDictionaryWordCommandImpl extends AbstractDictionaryCommand
		implements DeleteProjectDictionaryWordCommand, AuthorizableCommand {

	private Project project;
	private Long wordId;
	private User editedBy;

	@Autowired
	public DeleteProjectDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
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
	public void setWordId(Long wordId) {
		this.wordId = wordId;
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

	/**
	 * The repository matches on the project id as well as the word id, so an id from another
	 * project removes nothing and is reported as not found.
	 */
	@Override
	public void execute() {
		if (getProject() == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Deleting);
		}
		if (wordId == null) {
			throw EntityValidationException.emptyRequiredProperty(ProjectDictionaryWord.class,
					null, "wordId", EntityExceptionActionType.Deleting);
		}
		if (!getDictionaryRepository().deleteProjectWord(getProject().getId(), wordId)) {
			throw NoSuchEntityException.byQuery(ProjectDictionaryWord.class, "id", wordId);
		}
	}
}
