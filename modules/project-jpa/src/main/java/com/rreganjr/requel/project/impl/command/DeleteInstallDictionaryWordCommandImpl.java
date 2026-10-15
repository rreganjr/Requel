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
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.command.DeleteInstallDictionaryWordCommand;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.validator.EntityValidationException;

/**
 * Removes an installation-wide dictionary word (issue #319). Administrators only; see
 * {@link AddInstallDictionaryWordCommandImpl} for why it lives here.
 *
 * @author ron
 */
@Controller("deleteInstallDictionaryWordCommand")
@Scope("prototype")
public class DeleteInstallDictionaryWordCommandImpl extends AbstractDictionaryCommand
		implements DeleteInstallDictionaryWordCommand, AuthorizableCommand {

	private Long wordId;
	private User editedBy;

	@Autowired
	public DeleteInstallDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
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
		return new RequiresSystemRole(SystemAdminUserRole.class);
	}

	@Override
	public void execute() {
		if (wordId == null) {
			throw EntityValidationException.emptyRequiredProperty(InstallDictionaryWord.class,
					null, "wordId", EntityExceptionActionType.Deleting);
		}
		if (!getDictionaryRepository().deleteInstallWord(wordId)) {
			throw NoSuchEntityException.byQuery(InstallDictionaryWord.class, "id", wordId);
		}
	}
}
