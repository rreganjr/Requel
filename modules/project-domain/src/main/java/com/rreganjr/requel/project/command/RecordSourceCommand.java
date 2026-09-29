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
import com.rreganjr.requel.project.ProvenanceStore.RecordedSource;
import com.rreganjr.requel.project.SourceLocatorType;

/**
 * Issue #272: create or update a project's external source, identified by (system, externalId).
 * Supplying a content hash records the source's current version; the result says whether it
 * changed since the last one recorded. Requires {@code Project[Edit]}.
 */
public interface RecordSourceCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	void setSystem(String system);

	void setExternalId(String externalId);

	void setLocatorType(SourceLocatorType locatorType);

	void setLocator(String locator);

	void setTitle(String title);

	void setContentHash(String contentHash);

	RecordedSource getRecordedSource();
}
