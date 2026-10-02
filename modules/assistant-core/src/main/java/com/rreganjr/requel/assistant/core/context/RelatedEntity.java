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
package com.rreganjr.requel.assistant.core.context;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Issue #261: an entity a {@link ContextProvider} adds to a pack, with how it relates to the
 * target ({@code "Refined by"}, {@code "sibling goal"}, {@code "step 3 of 7"}) and, where the
 * shape needs it, its own children (a scenario's steps).
 */
public record RelatedEntity(EntityRef ref, String relation, String name, String text,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) List<RelatedEntity> children) {

	public RelatedEntity {
		Objects.requireNonNull(ref, "ref");
		children = children == null ? List.of() : List.copyOf(children);
	}

	public RelatedEntity(EntityRef ref, String relation, String name, String text) {
		this(ref, relation, name, text, List.of());
	}

	/** Characters this entity (and its children) adds, as the budget counts them. */
	public int size() {
		int total = length(relation) + length(name) + length(text);
		for (RelatedEntity child : children) {
			total += child.size();
		}
		return total;
	}

	private static int length(String value) {
		return value == null ? 0 : value.length();
	}
}
