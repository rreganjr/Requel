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
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.AddProjectDictionaryWordCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * Adds a word to a project's dictionary from its Dictionary page (issue #319).
 * <p>
 * Extends {@link AbstractDictionaryCommand}, not {@code AbstractProjectCommand}, for the reason
 * {@link EditDictionaryWordCommandImpl} gives: the project command bases are
 * {@code AuthorizationExemptable}, which would skip the gate below. It holds the {@link Project}
 * the caller resolved, again as {@code EditDictionaryWordCommandImpl} explains.
 *
 * @author ron
 */
@Controller("addProjectDictionaryWordCommand")
@Scope("prototype")
public class AddProjectDictionaryWordCommandImpl extends AbstractDictionaryCommand
		implements AddProjectDictionaryWordCommand, AuthorizableCommand {

	private Project project;
	private String lemma;
	private User editedBy;
	private ProjectDictionaryWord word;

	@Autowired
	public AddProjectDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
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
	public void setLemma(String lemma) {
		this.lemma = lemma;
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
	public ProjectDictionaryWord getWord() {
		return word;
	}

	/**
	 * Managing the word list is a project edit. The assistant stakeholder holds only annotation
	 * permissions, so it is refused here while it can still add a word by resolving an issue.
	 */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Project.class, "Edit");
	}

	@Override
	public void execute() {
		if (getProject() == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Updating);
		}
		String normalized = DictionaryWordRules.normalize(ProjectDictionaryWord.class, lemma);
		Long projectId = getProject().getId();
		getDictionaryRepository().addToDictionary(projectId, normalized);
		word = getDictionaryRepository().findProjectWord(projectId, normalized);
	}
}
