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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

import com.rreganjr.requel.annotation.spi.DefaultAnnotatableTypeRegistry;
import com.rreganjr.requel.annotation.spi.DefaultGroupingObjectRegistry;
import com.rreganjr.requel.project.impl.ProjectImpl;

public class ProjectAnnotatableRegistryConfigurationTest {

	@Test
	public void registersProjectAnnotatableAndGroupingTypes() throws Exception {
		DefaultAnnotatableTypeRegistry annotatableRegistry = new DefaultAnnotatableTypeRegistry();
		DefaultGroupingObjectRegistry groupingRegistry = new DefaultGroupingObjectRegistry();

		ProjectAnnotatableRegistryConfiguration configuration = new ProjectAnnotatableRegistryConfiguration();
		InitializingBean initializer = configuration.projectAnnotatableRegistryInitializer(
				annotatableRegistry, groupingRegistry);
		initializer.afterPropertiesSet();

		assertTrue(annotatableRegistry
				.resolveEntityType("com.rreganjr.requel.project.Project")
				.map(ProjectImpl.class::equals)
				.orElse(false));

		assertEquals("com.rreganjr.requel.project.Project",
				groupingRegistry.resolveDiscriminator(ProjectImpl.class).orElse(null));
	}
}
