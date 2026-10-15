/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.requel.command.AnalyzableEditCommand;
import com.rreganjr.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public interface EditProjectOrDomainEntityCommand extends AnalyzableEditCommand {

	/**
	 * The name of the "name" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * Set the project or domain this entity is a part of.
	 *
	 * @param projectOrDomain
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * @return the project or domain this entity belongs to
	 */
	public ProjectOrDomain getProjectOrDomain();

	/**
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Set the caller-supplied optimistic-lock version expected for the entity being
	 * edited. When non-null on an update, the command compares it against the
	 * entity's currently persisted version and fails with an
	 * {@link com.rreganjr.platform.exception.EntityLockException} if the entity has
	 * changed since the caller loaded it, rather than silently overwriting the
	 * concurrent edit. A {@code null} value (or a create, where no entity is
	 * resolved) skips the check. See issue #108.
	 *
	 * @param expectedVersion the version the caller believes is current, or
	 *        {@code null} to skip the optimistic-lock check
	 */
	public void setExpectedVersion(Integer expectedVersion);
}
