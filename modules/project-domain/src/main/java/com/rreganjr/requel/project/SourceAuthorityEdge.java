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
package com.rreganjr.requel.project;

import java.util.Date;

import com.rreganjr.platform.identity.User;

/**
 * Issue #273: one project source defers to another. Where the two disagree, the
 * {@linkplain #getSuperior() superior} wins; both stay current. This is precedence, not
 * obsolescence — the production guide that says "if this and RUNBOOK.md disagree, the repository
 * is correct" is still the guide people read.
 * <p>
 * Edges form a directed acyclic graph within one project: a source may defer to several others,
 * and precedence is transitive ({@link SourceAuthority}).
 */
public interface SourceAuthorityEdge {

	Long getId();

	Long getProjectId();

	/** The source that gives way. */
	ExternalSource getSubordinate();

	/** The source that wins where the two disagree. */
	ExternalSource getSuperior();

	/**
	 * A person's note on the edge, e.g. "operational detail only", or null. Recorded and shown;
	 * nothing evaluates it.
	 */
	String getNote();

	User getCreatedBy();

	Date getDateCreated();
}
