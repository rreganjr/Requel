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

/**
 * Optional rules overlay for a tag {@code category} (issue #112, Phase 6). A category is identified
 * by name within a scope (a project, or global when {@link #getProjectId()} is null) and may declare:
 *
 * <ul>
 *   <li>{@link #isExclusive() exclusivity} — an entity may hold at most one tag from this category;</li>
 *   <li>{@link #getAllowedEntityTypes() allowed entity types} — restrict which entity types its tags
 *       may attach to (empty = any);</li>
 *   <li>{@link #getValues() a controlled value list} — the only values permitted (empty = any);</li>
 *   <li>{@link #getColor() a fallback colour} — used when a tag has no colour of its own.</li>
 * </ul>
 *
 * Where no category row matches a tag's category name, no rules apply (pre-Phase-6 behaviour).
 *
 * @author ron
 */
public interface TagCategory extends CreatedEntity {

	Long getId();

	int getVersion();

	/** Owning project id, or {@code null} for a global category. */
	Long getProjectId();

	/** The category name (matches {@code tag.category}), e.g. {@code type}. */
	String getName();

	/** Whether an entity may hold at most one tag from this category. */
	boolean isExclusive();

	/** Fallback UI colour, or {@code null}. */
	String getColor();

	/** Allowed entity-type discriminators; empty means any entity type. */
	Set<String> getAllowedEntityTypes();

	/** The controlled value list (normalized slugs); empty means any value. */
	Set<String> getValues();
}
