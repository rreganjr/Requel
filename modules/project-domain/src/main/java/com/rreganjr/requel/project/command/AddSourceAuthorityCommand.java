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
import com.rreganjr.requel.project.SourceAuthorityEdge;

/**
 * Issue #273: record that one project source defers to another — where the two disagree, the
 * other wins, and both stay current. Idempotent on the pair (a repeat updates the note).
 * Self-edges, cycles and sources of another project are refused. Requires {@code Project[Edit]}.
 */
public interface AddSourceAuthorityCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	/** The source that gives way. */
	void setSystem(String system);

	void setExternalId(String externalId);

	/** The source that wins where the two disagree. */
	void setDefersToSystem(String system);

	void setDefersToExternalId(String externalId);

	/** A note on the edge; null leaves an existing note alone, blank clears it (#316). */
	void setNote(String note);

	SourceAuthorityEdge getEdge();
}
