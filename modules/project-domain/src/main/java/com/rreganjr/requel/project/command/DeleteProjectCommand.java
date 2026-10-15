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

/**
 * Delete a whole project and every entity it contains (issue #240, epic #239).
 *
 * @author ron
 */
public interface DeleteProjectCommand extends EditCommand {

	/**
	 * Set the project to delete.
	 *
	 * @param project
	 */
	public void setProject(Project project);

	/**
	 * The caller-supplied optimistic-lock version the project must still be at
	 * for the delete to proceed (issue #108). When {@code null} the version
	 * check is skipped.
	 *
	 * @param expectedVersion
	 */
	public void setExpectedVersion(Integer expectedVersion);
}
