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
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.SourceLinkRelation;

/**
 * Issue #272: record that an entity was derived from a fragment of an existing source, as the
 * entity is now — or, issue #273, that it cites the source. The low-level primitive: it records without deciding anything, so it never
 * raises a conflict — {@link UpsertFromSourceCommand} is the ingest path. Requires the entity's
 * {@code Edit} permission.
 */
public interface LinkSourceCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	void setTarget(ProjectOrDomainEntity target);

	void setSystem(String system);

	void setExternalId(String externalId);

	/** The fragment of the source, or null for the whole source. */
	void setFragment(String fragment);

	/** The fragment's text as ingested, hashed to detect a later change; may be null. */
	void setFragmentText(String fragmentText);

	/**
	 * Issue #273: the relation to record; null means {@link SourceLinkRelation#DERIVED_FROM}. A
	 * {@link SourceLinkRelation#CITES} link carries no hashes, so fragment text is refused.
	 */
	void setRelation(SourceLinkRelation relation);

	EntitySourceLink getLink();
}
