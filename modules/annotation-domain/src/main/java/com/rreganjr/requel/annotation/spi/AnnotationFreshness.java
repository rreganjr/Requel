/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation.spi;

import java.util.Collection;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;

/**
 * Issue #270: which annotations "may no longer apply" on an entity. An assistant's finding records
 * the text it was derived from; the finding is stale when the entity's text has changed since, or
 * when a later run stopped reporting it but a human's discussion kept the annotation. Staleness is
 * worked out when read, so it is right from the moment an edit commits, with nothing written on
 * edit.
 *
 * <p>
 * Implemented by the assistant module, which owns the findings; readers in modules that don't
 * depend on it (the REST and gateway DTO mappers) use this interface, falling back to
 * {@link #NONE} when no implementation is present.
 */
public interface AnnotationFreshness {

	/** Nothing is ever stale: for contexts without the assistant module. */
	AnnotationFreshness NONE = annotatables -> StaleAnnotations.NONE;

	/**
	 * Work out staleness for every annotation on {@code annotatables}, in one lookup.
	 *
	 * @return the answer for those annotatables; asking about any other one answers false.
	 */
	StaleAnnotations staleAnnotations(Collection<? extends Annotatable> annotatables);

	/** The result of one {@link #staleAnnotations} lookup. */
	@FunctionalInterface
	interface StaleAnnotations {

		StaleAnnotations NONE = (annotatable, annotation) -> false;

		/**
		 * @return true if {@code annotation}, on {@code annotatable}, may no longer apply. An
		 *         annotation with no assistant finding behind it (written by a person) is never
		 *         stale. Stale is per entity: an annotation shared by several entities can be
		 *         stale on one and not on another.
		 */
		boolean isStale(Annotatable annotatable, Annotation annotation);
	}
}
