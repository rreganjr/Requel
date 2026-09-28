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
 * Issue #268: re-run the assistants on every text entity of a project. The command itself
 * changes nothing; after it succeeds {@code AnalysisInvokingCommandHandler} queues one analysis
 * run per entity as a single batch. Requires {@code Annotation[Edit]}, since the runs write
 * issues on the user's behalf.
 */
public interface AnalyzeProjectCommand extends EditCommand, ProjectScopedCommand,
		ProjectAnalysisRequestSource {

	void setProject(Project project);
}
