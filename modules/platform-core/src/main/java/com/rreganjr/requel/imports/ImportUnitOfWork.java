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
