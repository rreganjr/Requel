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
