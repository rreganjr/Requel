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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.rreganjr.platform.identity.User;

/**
 * Issue #272: where external sources and entity-source links live. Both are side tables keyed by
 * entity type and id, like {@code ignored_findings}, so nothing in the entity graph reaches them
 * and no assistant context pack can include them. The entity and project delete paths remove
 * their rows explicitly.
 */
public interface ProvenanceStore {

	/** Longest accepted locator. */
	int MAX_LOCATOR_LENGTH = 2048;

	/** Longest accepted system, external id, title and fragment. */
	int MAX_SYSTEM_LENGTH = 40;
	int MAX_EXTERNAL_ID_LENGTH = 255;
	int MAX_TITLE_LENGTH = 255;
	int MAX_FRAGMENT_LENGTH = 255;

	/**
	 * What to record about a source. Null {@code locatorType}/{@code locator}/{@code title}/
	 * {@code contentHash} leave an existing source's values unchanged.
	 */
	record SourceSpec(Long projectId, String system, String externalId,
			SourceLocatorType locatorType, String locator, String title, String contentHash) {
	}

	/**
	 * The recorded source. {@code changed} is true when a content hash was supplied and differs
	 * from the one recorded before (a new source reports false: there was nothing to change from).
	 */
	record RecordedSource(ExternalSource source, boolean created, boolean changed) {
	}

	/** What to record about a link; an existing link with the same key is updated in place. */
	record LinkSpec(ExternalSource source, SourceLinkRelation relation, String targetType,
			Long targetId, String fragment, String fragmentHash, String sourceHashSeen,
			String entityFingerprint) {
	}

	/** Create or update the source identified by (project, system, externalId). */
	RecordedSource recordSource(SourceSpec spec, User by);

	Optional<ExternalSource> findSource(Long projectId, String system, String externalId);

	/** The project's sources, ordered by system then external id. */
	List<ExternalSource> listSources(Long projectId);

	/** Every link from one entity, ordered by source then fragment. */
	List<EntitySourceLink> linksForTarget(String targetType, Long targetId);

	/** Every link to one source, ordered by fragment then entity. */
	List<EntitySourceLink> linksForSource(Long sourceId);

	/**
	 * The links for one fragment of a source ({@code null} fragment = the whole source), of one
	 * relation and, when {@code targetType} is not null, one entity type.
	 */
	List<EntitySourceLink> linksForFragment(Long sourceId, SourceLinkRelation relation,
			String fragment, String targetType);

	/** Create or update the link; its key is (source, relation, target, fragment). */
	EntitySourceLink link(LinkSpec spec, User by);

	/** Fill in a link's entity fingerprint without touching anything else (#272 P6). */
	void recordFingerprint(Long linkId, String entityFingerprint);

	/** Restore when a source was last ingested, e.g. from an imported project file. */
	void restoreLastIngestedAt(Long sourceId, java.util.Date lastIngestedAt);

	/** Restore when a link was last ingested, e.g. from an imported project file. */
	void restoreIngestedAt(Long linkId, java.util.Date ingestedAt);

	/** @return true if a matching link was deleted. */
	boolean unlink(Long sourceId, SourceLinkRelation relation, String targetType, Long targetId,
			String fragment);

	/** Delete the links from one entity, e.g. when it is deleted. */
	int deleteForTarget(String targetType, Long targetId);

	/** Delete a project's sources and links, when it is deleted. */
	int deleteForProject(Long projectId);

	/** The stored form of a system: trimmed and lower-cased. */
	static String normalizeSystem(String system) {
		return system == null ? null : system.strip().toLowerCase(Locale.ROOT);
	}

	/** The stored form of a fragment: trimmed, blank = null (the whole source). */
	static String normalizeFragment(String fragment) {
		if (fragment == null) {
			return null;
		}
		String stripped = fragment.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	/** The fragment as it takes part in the unique key: the fragment, or "" for the whole source. */
	static String fragmentKey(String fragment) {
		String normalized = normalizeFragment(fragment);
		return normalized == null ? "" : normalized;
	}

	/**
	 * Check what a source spec can be checked for without the database: a system and external id
	 * within their lengths, and a locator acceptable for its type. Commands call this before the
	 * store, so a bad value is the caller's error rather than a persistence failure.
	 *
	 * @throws IllegalArgumentException naming the first problem
	 */
	static void validateSourceSpec(SourceSpec spec) {
		String system = normalizeSystem(spec.system());
		String externalId = spec.externalId() == null ? null : spec.externalId().strip();
		if (system == null || system.isEmpty()) {
			throw new IllegalArgumentException("system is required");
		}
		if (externalId == null || externalId.isEmpty()) {
			throw new IllegalArgumentException("externalId is required");
		}
		if (system.length() > MAX_SYSTEM_LENGTH) {
			throw new IllegalArgumentException("system is longer than " + MAX_SYSTEM_LENGTH);
		}
		if (externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
			throw new IllegalArgumentException("externalId is longer than " + MAX_EXTERNAL_ID_LENGTH);
		}
		validateLocator(spec.locatorType(), spec.locator());
	}

	/**
	 * Check a locator against its type (#272 P7).
	 *
	 * @throws IllegalArgumentException when the locator is not acceptable for the type
	 */
	static void validateLocator(SourceLocatorType type, String locator) {
		if (locator == null) {
			return;
		}
		if (type == null) {
			throw new IllegalArgumentException("locatorType is required when a locator is given");
		}
		if (locator.isBlank()) {
			throw new IllegalArgumentException("locator must not be blank");
		}
		if (locator.length() > MAX_LOCATOR_LENGTH) {
			throw new IllegalArgumentException(
					"locator is longer than " + MAX_LOCATOR_LENGTH + " characters");
		}
		switch (type) {
			case URL -> {
				URI uri;
				try {
					uri = new URI(locator.strip());
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException("locator is not a valid URL: " + e.getReason());
				}
				String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
				if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null) {
					throw new IllegalArgumentException(
							"a URL locator must be an absolute http or https URL");
				}
			}
			case PATH -> {
				String path = locator.strip().replace('\\', '/');
				if (path.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
					throw new IllegalArgumentException(
							"a PATH locator must be a relative path, not a URL or a drive path");
				}
				if (path.startsWith("/")) {
					throw new IllegalArgumentException("a PATH locator must be relative");
				}
				for (String segment : path.split("/")) {
					if ("..".equals(segment)) {
						throw new IllegalArgumentException(
								"a PATH locator must not contain a '..' segment");
					}
				}
			}
		}
	}
}
