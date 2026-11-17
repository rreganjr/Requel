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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Idea;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Submit a rough requirement idea to a project, as typed text and/or the text of an uploaded
 * file. The project must exist. Requires {@code Project[Edit]}.
 */
public interface SubmitIdeaCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	/** The typed idea text; may be null when a file supplies the text. */
	void setText(String text);

	/** The text of an uploaded file and its name; both null when no file was uploaded. */
	void setFileText(String fileText, String fileName);

	/** @return the saved idea, once executed */
	Idea getIdea();
}
