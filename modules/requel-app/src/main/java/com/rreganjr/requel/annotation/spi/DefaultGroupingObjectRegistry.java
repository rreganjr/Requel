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
package com.rreganjr.requel.annotation.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Simple in-memory implementation of {@link GroupingObjectRegistry}.
 */
@Component
public class DefaultGroupingObjectRegistry implements GroupingObjectRegistry {

    private final Map<String, Class<?>> byDiscriminator = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> byType = new ConcurrentHashMap<>();

    @Override
    public void registerGroupingType(String discriminator, Class<?> groupingType) {
        Objects.requireNonNull(discriminator, "discriminator must not be null");
        Objects.requireNonNull(groupingType, "groupingType must not be null");

        String existing = byType.putIfAbsent(groupingType, discriminator);
        if (existing != null && !existing.equals(discriminator)) {
            throw new IllegalArgumentException("Grouping type " + groupingType.getName()
                    + " already registered with discriminator " + existing);
        }

        Class<?> previous = byDiscriminator.putIfAbsent(discriminator, groupingType);
        if (previous != null && !previous.equals(groupingType)) {
            throw new IllegalArgumentException("Discriminator " + discriminator
                    + " already registered for grouping type " + previous.getName());
        }
    }

    @Override
    public Optional<Class<?>> resolveGroupingType(String discriminator) {
        return Optional.ofNullable(byDiscriminator.get(discriminator));
    }

    @Override
    public Optional<String> resolveDiscriminator(Class<?> groupingType) {
        return Optional.ofNullable(byType.get(groupingType));
    }

    @Override
    public Map<String, Class<?>> getRegisteredGroupingTypes() {
        return Collections.unmodifiableMap(byDiscriminator);
    }
}
