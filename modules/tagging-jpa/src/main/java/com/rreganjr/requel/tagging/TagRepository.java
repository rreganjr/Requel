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
package com.rreganjr.requel.tagging;

import java.util.List;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.repository.Repository;

/**
 * Persistence entry point for {@link Tag}s.
 *
 * @author ron
 */
public interface TagRepository extends Repository {

	/**
	 * Persist a new tag.
	 *
	 * @param tag the transient tag to create
	 * @return the managed tag
	 * @throws EntityException if the tag cannot be created
	 */
	Tag createTag(Tag tag) throws EntityException;

	/**
	 * @param id the tag id
	 * @return the tag, or {@code null} if none exists with that id
	 */
	Tag findTagById(Long id);

	/**
	 * Look up a tag by its normalized identity within a scope.
	 *
	 * @param projectId owning project id, or {@code null} for global
	 * @param category the (already-normalized) category, or {@code null} for a flat tag
	 * @param value the (already-normalized) value
	 * @return the matching tag, or {@code null} if none exists
	 */
	Tag findTag(Long projectId, String category, String value);

	/**
	 * @param projectId the project id, or {@code null} for global-only
	 * @return the project-scoped tags plus the global tags (when {@code projectId} is
	 *         non-null), ordered by category then value
	 */
	List<Tag> findTagsForProject(Long projectId);

	/**
	 * @param taggableType the registry discriminator (e.g. {@code "Goal"})
	 * @param taggableId the tagged entity's id
	 * @return the tags assigned to that entity
	 */
	List<Tag> findTagsOnEntity(String taggableType, Long taggableId);

	/**
	 * @param projectId the project id, or {@code null} for global-only
	 * @return the distinct non-null categories in scope, for autocomplete
	 */
	List<String> findDistinctCategories(Long projectId);
}
