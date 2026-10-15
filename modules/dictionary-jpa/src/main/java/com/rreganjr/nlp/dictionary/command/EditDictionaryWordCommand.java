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
package com.rreganjr.nlp.dictionary.command;

import com.rreganjr.platform.command.EditCommand;

/**
 * Create or Edit a word in a project's dictionary.
 * <p>
 * Not registered in any {@code *CommandRegistrar}, so it is not callable over
 * {@code /api/commands}, and absent from both lists in {@code GatewayPolicyConfig} because there
 * is nothing to allow or deny. If it is ever registered it belongs on
 * {@code GatewayPolicyConfig.DENIED}: it is reached today only through
 * {@code ResolveIssueWithAddWordToDictionaryPositionCommand}.
 * <p>
 * An {@link EditCommand} since issue #312, because the implementation is an
 * {@code AuthorizableCommand} and {@code AuthorizingCommandHandler} reads the permission check's
 * subject from {@code getEditedBy()}.
 *
 * @author ron
 */
public interface EditDictionaryWordCommand extends EditCommand {

	/**
	 * The text of the word.
	 * 
	 * @param lemma
	 */
	public void setLemma(String lemma);

	/**
	 * The project whose dictionary the word is added to (issue #313).
	 * <p>
	 * Required since issue #312. A null projectId used to mean "add to the installation-wide
	 * dictionary", which was the last ungated installation-wide write in the tree; the
	 * implementation now refuses it rather than writing outside any project.
	 *
	 * @param projectId
	 */
	public void setProjectId(Long projectId);
}
