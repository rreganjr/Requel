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

import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Adds a word to a project's dictionary from the project's Dictionary page (issue #319).
 * <p>
 * Distinct from {@link EditProjectDictionaryWordCommand}, which the "Add to Dictionary" resolve
 * runs and which is gated on {@code Annotation[Edit]} so that anyone who can resolve the lexical
 * issue can add the word. Managing the list directly is a project edit, gated on
 * {@code Project[Edit]}; the assistant stakeholder holds only annotation permissions, so it
 * cannot.
 * <p>
 * Adding a word the project already has, in any case, is a no-op that returns the existing row.
 *
 * @author ron
 */
public interface AddProjectDictionaryWordCommand extends EditCommand, ProjectScopedCommand {

	/**
	 * @param project -
	 *            the project whose dictionary receives the word; the instance the caller resolved.
	 */
	public void setProject(Project project);

	/**
	 * @param lemma -
	 *            the word. Trimmed; must be non-blank, one token and at most 80 characters.
	 */
	public void setLemma(String lemma);

	/**
	 * @return the added (or already present) word, after execution.
	 */
	public ProjectDictionaryWord getWord();
}
