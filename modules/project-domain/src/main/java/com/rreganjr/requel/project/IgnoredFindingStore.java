/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.rreganjr.platform.identity.User;

/**
 * Issue #320: the ignored assistant findings of each project. The interface is in the domain so
 * the assistant applicator (assistant-core) and the resolve hook (requel-app) can use it; the
 * JPA implementation, and the delete and export/import paths, are in project-jpa.
 */
public interface IgnoredFindingStore {

	/** What to record. {@code keySuffix} is the idempotency key after the entity's id. */
	record Spec(Long projectId, String targetType, Long targetId, String assistantId,
			String findingType, String propertyName, String keySuffix, String subject,
			Long annotationId) {

		public String idempotencyKey() {
			return IgnoredFinding.idempotencyKey(assistantId, targetType, targetId, keySuffix);
		}
	}

	/**
	 * The lower-cased idempotency keys ignored in the project. One query, meant to be read once
	 * per assistant run.
	 */
	Set<String> ignoredKeys(Long projectId);

	/**
	 * Record an ignore. Idempotent on the lower-cased key: recording the same finding again, in
	 * any case, returns the existing row.
	 */
	IgnoredFinding record(Spec spec, User createdBy);

	/** The project's ignores, ordered by entity then subject. */
	List<IgnoredFinding> list(Long projectId);

	Optional<IgnoredFinding> find(Long projectId, Long id);

	/** @return true if a row in {@code projectId} with {@code id} was deleted. */
	boolean delete(Long projectId, Long id);

	/** Delete the ignores on one entity, e.g. when it is deleted. */
	int deleteForTarget(String targetType, Long targetId);

	/** Delete a project's ignores, when it is deleted. */
	int deleteForProject(Long projectId);
}
