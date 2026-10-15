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
