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
 * Issue #272: an external artifact a project's entities were built from — a Jira ticket, a
 * guide, a review — recorded once per project. Requel is authoritative: the source is a pointer,
 * never read by Requel and never sent to a model.
 * <p>
 * Identity is ({@code projectId}, {@code system}, {@code externalId}). {@code system} is a
 * lower-cased vocabulary word ({@code jira}, {@code github}, {@code doc}); {@code externalId} is
 * kept exactly as given, unlike a tag (#255), because it has to round-trip to the source system.
 */
public interface ExternalSource {

	Long getId();

	Long getProjectId();

	/** The lower-cased source family, e.g. {@code jira}. */
	String getSystem();

	/** The source's own identifier, exactly as given, e.g. {@code CON-3685}. */
	String getExternalId();

	/** How to read {@link #getLocator()}, or null when there is no locator. */
	SourceLocatorType getLocatorType();

	/** Where the source is: a URL or a relative path. Never passed to a model. */
	String getLocator();

	/** A human title for the source, or null. */
	String getTitle();

	/** Reserved for #273's reference kinds; null for now. */
	String getKind();

	/**
	 * The caller's hash of the source's content as of the last ingest (for a file, the SHA-256
	 * of its bytes), or null when the caller never supplied one. Comparing it is how a caller
	 * tells a changed source from an unchanged one without reading the entities.
	 */
	String getContentHash();

	Date getLastIngestedAt();

	User getCreatedBy();

	Date getDateCreated();
}
