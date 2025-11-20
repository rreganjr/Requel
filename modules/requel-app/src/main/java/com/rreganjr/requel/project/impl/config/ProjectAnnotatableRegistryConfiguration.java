/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.annotation.spi.GroupingObjectRegistry;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.GoalRelationImpl;
import com.rreganjr.requel.project.impl.NonUserStakeholderImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.ProjectTeamImpl;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.requel.project.impl.UserStakeholderImpl;

/**
 * Registers all project entities that participate in annotation polymorphism and exposes the
 * discriminator map to Hibernate's metadata contributor.
 */
@Configuration
public class ProjectAnnotatableRegistryConfiguration {

	private static final Map<String, Class<? extends Annotatable>> PROJECT_ANNOTATABLE_TYPES;
	private static final Map<String, Class<?>> PROJECT_GROUPING_TYPES;

	static {
		final Map<String, Class<? extends Annotatable>> mappings = new LinkedHashMap<>();
		mappings.put("com.rreganjr.requel.project.Project", ProjectImpl.class);
		mappings.put("com.rreganjr.requel.project.ProjectTeam", ProjectTeamImpl.class);
		mappings.put("com.rreganjr.requel.project.Goal", GoalImpl.class);
		mappings.put("com.rreganjr.requel.project.GoalRelation", GoalRelationImpl.class);
		mappings.put("com.rreganjr.requel.project.UseCase", UseCaseImpl.class);
		mappings.put("com.rreganjr.requel.project.Scenario", ScenarioImpl.class);
		mappings.put("com.rreganjr.requel.project.Step", StepImpl.class);
		mappings.put("com.rreganjr.requel.project.Story", StoryImpl.class);
		mappings.put("com.rreganjr.requel.project.Actor", ActorImpl.class);
		mappings.put("com.rreganjr.requel.project.GlossaryTerm", GlossaryTermImpl.class);
		mappings.put("com.rreganjr.requel.project.NonUserStakeholder", NonUserStakeholderImpl.class);
		mappings.put("com.rreganjr.requel.project.UserStakeholder", UserStakeholderImpl.class);
		PROJECT_ANNOTATABLE_TYPES = Collections.unmodifiableMap(mappings);
		ProjectAnnotatableMetadataContributor.registerAnnotatableTypes(PROJECT_ANNOTATABLE_TYPES);

		final Map<String, Class<?>> groupingMappings = new LinkedHashMap<>();
		groupingMappings.put("com.rreganjr.requel.project.Project", ProjectImpl.class);
		PROJECT_GROUPING_TYPES = Collections.unmodifiableMap(groupingMappings);
		ProjectAnnotatableMetadataContributor.registerGroupingTypes(PROJECT_GROUPING_TYPES);
	}

	@Bean
	InitializingBean projectAnnotatableRegistryInitializer(
			AnnotatableTypeRegistry annotatableRegistry,
			GroupingObjectRegistry groupingRegistry) {
		return () -> {
			PROJECT_ANNOTATABLE_TYPES.forEach(annotatableRegistry::registerAnnotatableType);
			PROJECT_GROUPING_TYPES.forEach(groupingRegistry::registerGroupingType);
		};
	}
}
