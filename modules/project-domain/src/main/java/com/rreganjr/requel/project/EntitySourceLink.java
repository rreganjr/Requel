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
 * Issue #272: a link from one project entity to the {@link ExternalSource} it came from, naming
 * the fragment of the source (an acceptance criterion, a page, a section). A fragment may produce
 * several entities, and an entity may come from several sources.
 * <p>
 * The link records the state of both sides at the last ingest, so that change on either side is
 * worked out when read rather than written when it happens:
 * <ul>
 * <li>{@code fragmentHash} — {@code CriterionHash} of the fragment text as ingested;</li>
 * <li>{@code sourceHashSeen} — the source's content hash at that ingest;</li>
 * <li>{@code entityFingerprint} — {@link TargetFingerprint} of the entity as that ingest left it.
 * Null only for a link converted from a #71 provenance note (V26), whose ingest text was never
 * recorded.</li>
 * </ul>
 */
public interface EntitySourceLink {

	Long getId();

	ExternalSource getSource();

	Long getProjectId();

	SourceLinkRelation getRelation();

	/** The entity's type, {@code getProjectOrDomainEntityInterface().getSimpleName()}. */
	String getTargetType();

	Long getTargetId();

	/** The fragment of the source, or null for the whole source. */
	String getFragment();

	String getFragmentHash();

	String getSourceHashSeen();

	String getEntityFingerprint();

	Date getIngestedAt();

	User getCreatedBy();

	/**
	 * True when the source has a newer version than the one this link last saw: the fragment was
	 * not part of the latest ingest, so it may have been removed or renamed upstream.
	 */
	default boolean isNotInLatestSource() {
		String current = getSource() == null ? null : getSource().getContentHash();
		return current != null && getSourceHashSeen() != null && !current.equals(getSourceHashSeen());
	}
}
