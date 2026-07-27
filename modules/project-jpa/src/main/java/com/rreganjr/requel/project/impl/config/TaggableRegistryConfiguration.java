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
package com.rreganjr.requel.project.impl.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;

/**
 * Registers the project entities that participate in tag-assignment polymorphism and
 * exposes the discriminator map both to Hibernate's metadata contributor (static, for
 * boot) and to the runtime {@link TaggableTypeRegistry} (for controllers).
 *
 * <p>Phase 1 registers {@code Goal} and {@code Project}; later phases extend this to
 * {@code Actor}, {@code Story}, {@code Scenario}, {@code Stakeholder}, and
 * {@code UseCase}. Mirrors {@link ProjectAnnotatableRegistryConfiguration}.</p>
 */
@Configuration
public class TaggableRegistryConfiguration {

	private static final Map<String, Class<? extends Taggable>> PROJECT_TAGGABLE_TYPES;

	static {
		final Map<String, Class<? extends Taggable>> mappings = new LinkedHashMap<>();
		mappings.put("Goal", GoalImpl.class);
		mappings.put("Project", ProjectImpl.class);
		PROJECT_TAGGABLE_TYPES = Collections.unmodifiableMap(mappings);
		TaggableMetadataContributor.registerTaggableTypes(PROJECT_TAGGABLE_TYPES);
	}

	@Bean
	InitializingBean projectTaggableRegistryInitializer(TaggableTypeRegistry taggableRegistry) {
		return () -> PROJECT_TAGGABLE_TYPES.forEach(taggableRegistry::registerTaggableType);
	}
}
