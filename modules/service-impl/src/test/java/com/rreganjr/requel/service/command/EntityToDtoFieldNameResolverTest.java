/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rreganjr.requel.service.api.dto.EditGlossaryTermInput;
import com.rreganjr.requel.service.api.dto.EditUseCaseInput;
import com.rreganjr.requel.service.api.dto.EditUserInput;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EntityToDtoFieldNameResolver} (issue #176).
 */
class EntityToDtoFieldNameResolverTest {

    private final EntityToDtoFieldNameResolver resolver = new EntityToDtoFieldNameResolver();

    @Test
    void translatesAnnotatedEntityPropertyToDtoField() {
        assertEquals("password", resolver.toDtoField(EditUserInput.class, "encryptedPassword"));
        assertEquals("userRoleNames", resolver.toDtoField(EditUserInput.class, "roles"));
        assertEquals("primaryActorName", resolver.toDtoField(EditUseCaseInput.class, "primaryActor"));
        assertEquals("canonicalTermId", resolver.toDtoField(EditGlossaryTermInput.class, "canonicalTerm"));
    }

    @Test
    void passesThroughAnUnannotatedName() {
        // A name that coincides between entity and DTO (the common case) is returned unchanged.
        assertEquals("name", resolver.toDtoField(EditUserInput.class, "name"));
    }

    @Test
    void passesThroughWhenInputClassIsNull() {
        assertEquals("encryptedPassword", resolver.toDtoField(null, "encryptedPassword"));
    }

    @Test
    void passesThroughForANonRecordClass() {
        assertEquals("anything", resolver.toDtoField(String.class, "anything"));
    }
}
