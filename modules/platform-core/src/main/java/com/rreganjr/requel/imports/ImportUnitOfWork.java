package com.rreganjr.requel.imports;

import java.util.Optional;

/**
 * Minimal contract for coordinating imported aggregates without exposing transport concerns.
 * Implementations typically hold caches keyed by external identifiers and coordinate
 * persistence on behalf of assemblers.
 */
public interface ImportUnitOfWork {

    /**
     * Resolve an already-imported reference by external identifier.
     */
    <T> Optional<T> resolve(Class<T> type, String externalId);

    /**
     * Register an aggregate instance against an external identifier for later resolution.
     */
    <T> void register(Class<T> type, String externalId, T instance);

}
