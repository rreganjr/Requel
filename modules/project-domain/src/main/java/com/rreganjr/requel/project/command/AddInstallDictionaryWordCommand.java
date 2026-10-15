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
package com.rreganjr.requel.project.command;

import com.rreganjr.nlp.dictionary.InstallDictionaryWord;
import com.rreganjr.platform.command.EditCommand;

/**
 * Adds a word to the installation-wide dictionary (issue #319): known in every project at once.
 * Requires the system administrator role, and is denied on the MCP gateway.
 * <p>
 * Declared here rather than in {@code dictionary-jpa} because its implementation names
 * {@code SystemAdminUserRole}, which {@code dictionary-jpa} cannot see.
 *
 * @author ron
 */
public interface AddInstallDictionaryWordCommand extends EditCommand {

	/**
	 * @param lemma -
	 *            the word. Trimmed; must be non-blank, one token and at most 80 characters.
	 */
	public void setLemma(String lemma);

	/**
	 * @return the added (or already present) word, after execution.
	 */
	public InstallDictionaryWord getWord();
}
