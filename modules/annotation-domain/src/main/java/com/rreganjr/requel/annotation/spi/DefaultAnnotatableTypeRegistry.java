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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.annotation.Annotatable;

/**
 * Default in-memory implementation backed by concurrent maps.
 *
 * @author ron
 */
@Component
public class DefaultAnnotatableTypeRegistry implements AnnotatableTypeRegistry {

	private final Map<String, Class<? extends Annotatable>> byDiscriminator = new ConcurrentHashMap<>();
	private final Map<Class<?>, String> byType = new ConcurrentHashMap<>();

	@Override
	public void registerAnnotatableType(String discriminator, Class<? extends Annotatable> entityType) {
		Objects.requireNonNull(discriminator, "discriminator must not be null");
		Objects.requireNonNull(entityType, "entityType must not be null");

		String previousDiscriminator = byType.putIfAbsent(entityType, discriminator);
		if ((previousDiscriminator != null) && !previousDiscriminator.equals(discriminator)) {
			throw new IllegalArgumentException("Entity type " + entityType.getName()
					+ " already registered with discriminator " + previousDiscriminator);
		}

		Class<? extends Annotatable> previousType = byDiscriminator.putIfAbsent(discriminator, entityType);
		if ((previousType != null) && !previousType.equals(entityType)) {
			throw new IllegalArgumentException("Discriminator " + discriminator
					+ " already registered for type " + previousType.getName());
		}
	}

	@Override
	public Optional<Class<? extends Annotatable>> resolveEntityType(String discriminator) {
		return Optional.ofNullable(byDiscriminator.get(discriminator));
	}

	@Override
	public Optional<String> resolveDiscriminator(Class<?> entityType) {
		return Optional.ofNullable(byType.get(entityType));
	}

	@Override
	public Map<String, Class<? extends Annotatable>> getRegisteredAnnotatableTypes() {
		return Collections.unmodifiableMap(byDiscriminator);
	}
}
