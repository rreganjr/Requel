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
 * Issue #273: delete one of a project's sources, with its citations and authority edges. Refused
 * while anything was derived from the source, so provenance never disappears as a side effect
 * of removing a reference. Requires {@code Project[Edit]}.
 */
public interface DeleteSourceCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	void setSystem(String system);

	void setExternalId(String externalId);

	/** @return how many citations were removed with the source. */
	long getCitationsRemoved();
}
