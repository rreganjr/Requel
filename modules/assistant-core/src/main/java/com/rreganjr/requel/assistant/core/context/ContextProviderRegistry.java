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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Issue #261: the context providers by id. {@link #ENTITY} is the base pack, always built; the
 * rest are {@link ContextProvider} beans. Definition validation checks provider ids against
 * {@link #ids()}.
 */
@Component
public class ContextProviderRegistry {

	/** The base pack: the entity itself, its human annotations and its glossary terms. */
	public static final String ENTITY = "entity";

	private final Map<String, ContextProvider> providers = new LinkedHashMap<>();

	@Autowired
	public ContextProviderRegistry(ObjectProvider<ContextProvider> beans) {
		this(beans.orderedStream().toList());
	}

	public ContextProviderRegistry(List<ContextProvider> beans) {
		for (ContextProvider provider : beans) {
			ContextProvider previous = providers.putIfAbsent(provider.id(), provider);
			if (previous != null || ENTITY.equals(provider.id())) {
				throw new IllegalStateException("duplicate context provider id " + provider.id());
			}
		}
	}

	/** Every id a definition may name, {@link #ENTITY} included, sorted. */
	public Set<String> ids() {
		Set<String> ids = new TreeSet<>(providers.keySet());
		ids.add(ENTITY);
		return ids;
	}

	/** The provider for {@code id}, or null ({@link #ENTITY} has none: it is the base pack). */
	public ContextProvider find(String id) {
		return providers.get(id);
	}
}
