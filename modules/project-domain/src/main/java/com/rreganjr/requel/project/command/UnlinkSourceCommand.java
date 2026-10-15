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
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.SourceLinkRelation;

/**
 * Issue #272: remove one derived-from (or, #273, citation) link from an entity. Nothing in the UI does this; it exists
 * for a link recorded by mistake. Requires the entity's {@code Edit} permission.
 */
public interface UnlinkSourceCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	void setTarget(ProjectOrDomainEntity target);

	void setSystem(String system);

	void setExternalId(String externalId);

	void setFragment(String fragment);

	/** Issue #273: the relation of the link to remove; null means DERIVED_FROM. */
	void setRelation(SourceLinkRelation relation);

	/** @return true if a link was removed. */
	boolean isUnlinked();
}
