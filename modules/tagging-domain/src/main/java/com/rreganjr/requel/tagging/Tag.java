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

import java.util.Set;

import com.rreganjr.platform.CreatedEntity;
import com.rreganjr.platform.domain.Describable;

/**
 * A reusable tag/category definition that can be attached to any {@link Taggable}
 * project entity.
 *
 * <p>A tag has an optional <em>category</em> (a.k.a. key / dimension / namespace,
 * e.g. {@code type}) and a required <em>value</em> (the label, e.g.
 * {@code business-rule}). A flat tag has a {@code null} category. A tag is scoped
 * either to a single project (its {@link #getProjectId() projectId}) or is global
 * (a {@code null} projectId, shared across projects).</p>
 *
 * @author ron
 */
public interface Tag extends CreatedEntity, Describable {

	/**
	 * @return the surrogate identity of the tag.
	 */
	Long getId();

	/**
	 * @return the optimistic-locking version.
	 */
	int getVersion();

	/**
	 * @return the optional category / key / dimension of the tag, or {@code null}
	 *         for a flat tag.
	 */
	String getCategory();

	/**
	 * @return the tag value / label; required and non-empty.
	 */
	String getValue();

	/**
	 * @return the id of the owning project, or {@code null} for a global/system tag.
	 *         Held as a soft reference (an id, not a mapped association) so the
	 *         tagging modules stay decoupled from the project module.
	 */
	Long getProjectId();

	/**
	 * @return an optional UI colour hint, or {@code null}.
	 */
	String getColor();

	/**
	 * @return the entities this tag is currently assigned to.
	 */
	Set<Taggable> getTaggables();
}
