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
package com.rreganjr.requel.tagging.spi;

import java.util.Map;
import java.util.Optional;

import com.rreganjr.requel.tagging.Taggable;

/**
 * Registry that tracks the mapping between tag-assignment discriminators and the
 * corresponding {@link Taggable} entity implementations.
 *
 * <p>The registry is the single source of truth for the polymorphic
 * {@code @ManyToAny} mapping used by the tag assignment. Domain modules that provide
 * new taggable entities should contribute registrations via Spring configuration
 * rather than modifying the tagging implementation package. This mirrors
 * {@code com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry}.</p>
 *
 * @author ron
 */
public interface TaggableTypeRegistry {

	/**
	 * Register a taggable entity for the supplied discriminator.
	 *
	 * @param discriminator the value stored in the {@code tag_taggable.taggable_type} column
	 * @param entityType the entity class that implements {@link Taggable}
	 * @throws IllegalArgumentException if the discriminator or entity class are already bound
	 *             to a different mapping
	 */
	void registerTaggableType(String discriminator, Class<? extends Taggable> entityType);

	/**
	 * Resolve the entity type for the supplied discriminator.
	 *
	 * @param discriminator the stored discriminator value
	 * @return optional containing the entity type, or empty if not registered
	 */
	Optional<Class<? extends Taggable>> resolveEntityType(String discriminator);

	/**
	 * Resolve the discriminator configured for the supplied entity type.
	 *
	 * @param entityType the entity class
	 * @return optional containing the discriminator, or empty if not registered
	 */
	Optional<String> resolveDiscriminator(Class<?> entityType);

	/**
	 * @return an immutable snapshot of the registered discriminator mappings
	 */
	Map<String, Class<? extends Taggable>> getRegisteredTaggableTypes();
}
