package com.rreganjr.requel.annotation.spi;

import java.util.Map;
import java.util.Optional;

/**
 * Registry of discriminator → grouping-object class mappings used by {@code AbstractAnnotation}.
 *
 * The grouping object represents the “owner” that aggregates a set of annotations (e.g. the project
 * that contains them). Modules that contribute new grouping types should register them here rather
 * than modifying the annotation implementation package.
 */
public interface GroupingObjectRegistry {

	void registerGroupingType(String discriminator, Class<?> groupingType);

	Optional<Class<?>> resolveGroupingType(String discriminator);

	Optional<String> resolveDiscriminator(Class<?> groupingType);

	Map<String, Class<?>> getRegisteredGroupingTypes();
}
