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

import com.rreganjr.requel.assistant.api.EntityRef;

/**
 * Focused context for a single target entity. Carries the entity snapshot,
 * its parent/child relationships, in-scope annotations, and any glossary
 * terms referenced by the entity's text. Built by an
 * {@link EntityContextPackBuilder} per analysis target.
 */
public record EntityContextPack(EntityRef target, EntitySnapshot snapshot, List<EntityRef> parents,
		List<EntityRef> children, List<AnnotationSnapshot> annotations,
		List<GlossaryTermSnapshot> relatedTerms, ContextPackMetadata metadata) {

	public EntityContextPack {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(metadata, "metadata");
		parents = parents == null ? List.of() : List.copyOf(parents);
		children = children == null ? List.of() : List.copyOf(children);
		annotations = annotations == null ? List.of() : List.copyOf(annotations);
		relatedTerms = relatedTerms == null ? List.of() : List.copyOf(relatedTerms);
	}
}
