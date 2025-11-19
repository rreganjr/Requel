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
