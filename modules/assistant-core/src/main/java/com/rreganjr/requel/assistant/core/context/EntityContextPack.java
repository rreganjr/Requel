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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Focused context for a single target entity. Carries the entity snapshot,
 * its parent/child relationships, in-scope annotations, and any glossary
 * terms referenced by the entity's text. Built by an
 * {@link EntityContextPackBuilder} per analysis target.
 *
 * <p>Issue #261: {@code context} holds the sections the run's definition asked for (goal
 * relations, a use case's scenarios, ...). It is left out of the JSON when empty, so a pack with
 * no providers serialises exactly as before.
 */
public record EntityContextPack(EntityRef target, EntitySnapshot snapshot, List<EntityRef> parents,
		List<EntityRef> children, List<AnnotationSnapshot> annotations,
		List<GlossaryTermSnapshot> relatedTerms,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) List<ContextSection> context,
		ContextPackMetadata metadata) {

	public EntityContextPack {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(metadata, "metadata");
		parents = parents == null ? List.of() : List.copyOf(parents);
		children = children == null ? List.of() : List.copyOf(children);
		annotations = annotations == null ? List.of() : List.copyOf(annotations);
		relatedTerms = relatedTerms == null ? List.of() : List.copyOf(relatedTerms);
		context = context == null ? List.of() : List.copyOf(context);
	}

	/** A pack with no provider sections. */
	public EntityContextPack(EntityRef target, EntitySnapshot snapshot, List<EntityRef> parents,
			List<EntityRef> children, List<AnnotationSnapshot> annotations,
			List<GlossaryTermSnapshot> relatedTerms, ContextPackMetadata metadata) {
		this(target, snapshot, parents, children, annotations, relatedTerms, List.of(), metadata);
	}

	/**
	 * This pack without its last provider section, noted in the metadata. The executor uses it
	 * to fit an over-cap pack, last provider first; the base pack is never dropped.
	 *
	 * @throws IllegalStateException if there is no section to drop
	 */
	public EntityContextPack withoutLastSection() {
		if (context.isEmpty()) {
			throw new IllegalStateException("no context section to drop");
		}
		ContextSection dropped = context.get(context.size() - 1);
		List<String> notes = new ArrayList<>(metadata.truncationNotes());
		notes.add(dropped.providerId() + ": dropped to fit the input cap");
		ContextPackMetadata trimmed = new ContextPackMetadata(metadata.builtAt(),
				metadata.totalCharacters(), true, metadata.redactedFields(), notes);
		return new EntityContextPack(target, snapshot, parents, children, annotations,
				relatedTerms, context.subList(0, context.size() - 1), trimmed);
	}
}
