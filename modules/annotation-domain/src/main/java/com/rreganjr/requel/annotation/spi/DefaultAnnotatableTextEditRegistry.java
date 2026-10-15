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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.annotation.Annotatable;

/**
 * Default in-memory implementation backed by a concurrent map.
 *
 * @author ron
 */
@Component
public class DefaultAnnotatableTextEditRegistry implements AnnotatableTextEditRegistry {

	private final Map<Class<? extends Annotatable>, AnnotatableTextEditor> byType =
			new ConcurrentHashMap<>();

	@Override
	public void registerTextEditor(Class<? extends Annotatable> entityType,
			AnnotatableTextEditor editor) {
		Objects.requireNonNull(entityType, "entityType must not be null");
		Objects.requireNonNull(editor, "editor must not be null");

		AnnotatableTextEditor previous = byType.putIfAbsent(entityType, editor);
		if ((previous != null) && !previous.equals(editor)) {
			throw new IllegalArgumentException("Entity type " + entityType.getName()
					+ " already has a text editor registered");
		}
	}

	@Override
	public Optional<AnnotatableTextEditor> resolveTextEditor(Class<?> entityType) {
		if (entityType == null) {
			return Optional.empty();
		}
		// Exact match first so a subclass with its own editor is not shadowed by its parent's
		// (ScenarioImpl extends StepImpl); then walk up, which is what resolves a Hibernate
		// proxy's generated class to the entity class it stands for.
		for (Class<?> type = entityType; type != null; type = type.getSuperclass()) {
			AnnotatableTextEditor editor = byType.get(type);
			if (editor != null) {
				return Optional.of(editor);
			}
		}
		return Optional.empty();
	}

	@Override
	public Map<Class<? extends Annotatable>, AnnotatableTextEditor> getRegisteredTextEditors() {
		return Collections.unmodifiableMap(new HashMap<>(byType));
	}
}
