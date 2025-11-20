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
package com.rreganjr.requel.imports;

/**
 * Domain-agnostic contract for completing an aggregate from an import draft.
 *
 * @param <D> draft type (format-agnostic input)
 * @param <A> aggregate type to assemble
 */
public interface AggregateAssembler<D, A> {

    Class<D> draftType();

    Class<A> aggregateType();

    /**
     * Assemble the target aggregate from the provided draft, using the supplied unit of work
     * for cross-aggregate reference resolution or caching.
     */
    A assemble(D draft, ImportUnitOfWork unitOfWork) throws ImportException;
}
