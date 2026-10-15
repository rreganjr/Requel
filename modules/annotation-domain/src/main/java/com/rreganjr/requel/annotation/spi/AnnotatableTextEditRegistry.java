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
package com.rreganjr.requel.annotation.spi;

import java.util.Map;
import java.util.Optional;

import com.rreganjr.requel.annotation.Annotatable;

/**
 * Maps an annotatable entity type to the {@link AnnotatableTextEditor} that can change its
 * text through that entity's own {@code Edit*Command} (issue #305).
 * <p>
 * Companion to {@link AnnotatableTypeRegistry} and populated the same way: domain modules
 * contribute their registrations through Spring configuration, so the annotation layer never
 * imports project implementation classes.
 * <p>
 * Only the entity types an analysing assistant can actually annotate need an entry. A type
 * with no editor is not a defect in itself &mdash; it simply cannot have its text corrected
 * by resolving a spelling issue, and the resolver refuses rather than editing it some other
 * way.
 *
 * @author ron
 */
public interface AnnotatableTextEditRegistry {

	/**
	 * Register the editor for an annotatable entity type.
	 *
	 * @param entityType the entity implementation class
	 * @param editor the editor that builds its edit command
	 * @throws IllegalArgumentException if that type is already bound to a different editor
	 */
	void registerTextEditor(Class<? extends Annotatable> entityType, AnnotatableTextEditor editor);

	/**
	 * Resolve the editor for an entity type.
	 * <p>
	 * Matches the exact type first, then walks up the superclass chain. Both matter: a lazily
	 * loaded entity arrives as a Hibernate proxy whose class is a generated subclass, and
	 * {@code ScenarioImpl} genuinely extends {@code StepImpl}, so an exact-match-only lookup
	 * would miss the proxy and a walk-only lookup would hand a scenario the step's editor.
	 *
	 * @param entityType the entity class, possibly a proxy or a subclass
	 * @return the editor, or empty when no registration applies
	 */
	Optional<AnnotatableTextEditor> resolveTextEditor(Class<?> entityType);

	/**
	 * @return an immutable snapshot of the registered editors
	 */
	Map<Class<? extends Annotatable>, AnnotatableTextEditor> getRegisteredTextEditors();
}
