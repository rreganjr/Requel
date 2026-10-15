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
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * @author ron
 */
public interface EditProjectCommand extends AnalyzableEditCommand, ProjectScopedCommand {

	/**
	 * The name of the "name" field used to correlate to the field in an editor
	 * and through exceptions.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * @param name
	 */
	public void setName(String name);

	/**
	 * @param description
	 */
	public void setText(String description);

	/**
	 * @param organizationId - the id of an existing organization
	 */
	public void setOrganizationId(Long organizationId);

	/**
	 * @param organizationName - the name of the organization (used to create a new one if organizationId is not set)
	 */
	public void setOrganizationName(String organizationName);

	/**
	 * @param project
	 */
	public void setProject(Project project);

	/**
	 * @return
	 */
	public Project getProject();

	/**
	 * Set the caller-supplied optimistic-lock version expected for the project being
	 * edited. When non-null on an update, the command compares it against the
	 * project's currently persisted version and fails with an
	 * {@link com.rreganjr.platform.exception.EntityLockException} if it has changed
	 * since the caller loaded it. A {@code null} value (or a create) skips the check.
	 * See issue #108.
	 *
	 * @param expectedVersion the version the caller believes is current, or
	 *        {@code null} to skip the optimistic-lock check
	 */
	public void setExpectedVersion(Integer expectedVersion);
}
