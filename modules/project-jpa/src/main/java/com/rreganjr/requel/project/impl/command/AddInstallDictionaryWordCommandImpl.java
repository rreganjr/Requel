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
import com.rreganjr.nlp.dictionary.InstallDictionaryWord;
import com.rreganjr.nlp.dictionary.impl.command.AbstractDictionaryCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.command.AddInstallDictionaryWordCommand;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Adds an installation-wide dictionary word (issue #319). Administrators only. It lives in
 * {@code project-jpa} because that is the lowest module that can name
 * {@link SystemAdminUserRole}.
 *
 * @author ron
 */
@Controller("addInstallDictionaryWordCommand")
@Scope("prototype")
public class AddInstallDictionaryWordCommandImpl extends AbstractDictionaryCommand
		implements AddInstallDictionaryWordCommand, AuthorizableCommand {

	private String lemma;
	private User editedBy;
	private InstallDictionaryWord word;

	@Autowired
	public AddInstallDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
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
	public InstallDictionaryWord getWord() {
		return word;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresSystemRole(SystemAdminUserRole.class);
	}

	@Override
	public void execute() {
		String normalized = DictionaryWordRules.normalize(InstallDictionaryWord.class, lemma);
		word = getDictionaryRepository().addInstallWord(normalized,
				editedBy == null ? null : editedBy.getId());
	}
}
