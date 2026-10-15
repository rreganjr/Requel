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
package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.ImportUnitOfWork;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * Simple in-memory ImportUnitOfWork for per-file import passes.
 */
public class DefaultImportUnitOfWork implements ImportUnitOfWork {

    private final Map<CacheKey, Object> cache = new ConcurrentHashMap<>();

    @Override
    public <T> Optional<T> resolve(Class<T> type, String externalId) {
        if (externalId == null) {
            return Optional.empty();
        }
        Object value = cache.get(CacheKey.of(type, externalId));
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    @Override
    public <T> void register(Class<T> type, String externalId, T instance) {
        if (externalId == null || instance == null) {
            return;
        }
        cache.put(CacheKey.of(type, externalId), instance);
    }

    private record CacheKey(Class<?> type, String externalId) {
        static CacheKey of(Class<?> type, String externalId) {
            Objects.requireNonNull(type, "cache key type is required");
            Objects.requireNonNull(externalId, "cache key externalId is required");
            return new CacheKey(type, externalId);
        }
    }
}
