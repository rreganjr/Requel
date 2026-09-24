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

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Removes one word from a project's dictionary (issue #319), gated on {@code Project[Edit]}.
 * <p>
 * The word is matched by id <em>and</em> project, so an id belonging to another project is
 * not-found and removes nothing. Text already analyzed is not re-checked: the word is flagged
 * again the next time the text is analyzed.
 *
 * @author ron
 */
public interface DeleteProjectDictionaryWordCommand extends EditCommand, ProjectScopedCommand {

	/**
	 * @param project -
	 *            the project whose dictionary holds the word; the instance the caller resolved.
	 */
	public void setProject(Project project);

	/**
	 * @param wordId -
	 *            the id of the project dictionary word to remove.
	 */
	public void setWordId(Long wordId);
}
