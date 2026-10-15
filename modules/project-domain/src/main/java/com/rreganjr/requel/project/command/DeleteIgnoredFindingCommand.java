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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Issue #320: stop ignoring an assistant finding. Deletes the ignored-finding row, unlinks the
 * resolved issue it came from from the entity (deleting the issue when nothing else has it) and
 * queues one analysis run for the entity, so the finding is raised again straight away.
 * Requires {@code Project[Edit]}, as the project dictionary page does (#319).
 */
public interface DeleteIgnoredFindingCommand extends EditCommand, ProjectScopedCommand {

	/** The project the ignore belongs to; an id from another project is not found. */
	void setProject(Project project);

	void setIgnoredFindingId(Long ignoredFindingId);
}
