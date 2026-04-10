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
package com.rreganjr.repository.jpa;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityLockException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExceptionMapper}.
 *
 * Uses the real {@link ExceptionMapper} constructed with its default adapter set.
 * No Spring context required.
 *
 * Key behaviour under test:
 * - EntityException passthrough (already domain exception)
 * - Unmapped RuntimeException passthrough
 * - Unmapped checked Exception wrapped in EntityException
 * - Optimistic-lock family (JPA, Hibernate, Spring) → EntityLockException
 * - EntityExistsException → EntityException (uniqueness conflict)
 * - jakarta.validation.ConstraintViolationException → BeanValidationException
 * - Nested cause chain: adapter found for a cause, not the top-level exception
 */
class ExceptionMapperTest {

    private ExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ExceptionMapper();
    }

    // -------------------------------------------------------------------------
    // Passthrough cases
    // -------------------------------------------------------------------------

    @Test
    void entityExceptionReturnedAsIs() {
        EntityException original = EntityException.uniquenessConflict(null, null, null, null);

        RuntimeException result = mapper.convertException(original);

        assertThat(result).isSameAs(original);
    }

    @Test
    void unmappedRuntimeExceptionReturnedAsIs() {
        IllegalArgumentException original = new IllegalArgumentException("bad arg");

        RuntimeException result = mapper.convertException(original);

        assertThat(result).isSameAs(original);
    }

    @Test
    void unmappedCheckedExceptionWrappedInEntityException() {
        Exception checked = new Exception("some checked exception");

        RuntimeException result = mapper.convertException(checked);

        assertThat(result).isInstanceOf(EntityException.class);
    }

    // -------------------------------------------------------------------------
    // Optimistic-lock family → EntityLockException
    // -------------------------------------------------------------------------

    @Test
    void jpaOptimisticLockExceptionMapsToEntityLockException() {
        OptimisticLockException ex = new OptimisticLockException("stale");

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(EntityLockException.class);
    }

    @Test
    void hibernateStaleObjectStateExceptionMapsToEntityLockException() {
        StaleObjectStateException ex = new StaleObjectStateException("SomeEntity", 42L);

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(EntityLockException.class);
    }

    @Test
    void springCannotAcquireLockExceptionMapsToEntityLockException() {
        CannotAcquireLockException ex = new CannotAcquireLockException("lock timeout");

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(EntityLockException.class);
    }

    @Test
    void springObjectOptimisticLockingFailureMapsToEntityLockException() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("SomeEntity", 1L);

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(EntityLockException.class);
    }

    // -------------------------------------------------------------------------
    // EntityExistsException → EntityException (uniqueness)
    // -------------------------------------------------------------------------

    @Test
    void entityExistsExceptionMapsToEntityException() {
        EntityExistsException ex = new EntityExistsException("duplicate");

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(EntityException.class);
    }

    // -------------------------------------------------------------------------
    // jakarta.validation.ConstraintViolationException → BeanValidationException
    // -------------------------------------------------------------------------

    @Test
    void beanValidationConstraintViolationMapsToBeanValidationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("username");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        jakarta.validation.ConstraintViolationException ex =
                new jakarta.validation.ConstraintViolationException(Set.of(violation));

        RuntimeException result = mapper.convertException(ex);

        assertThat(result).isInstanceOf(BeanValidationException.class);
        BeanValidationException bve = (BeanValidationException) result;
        assertThat(bve.getEntityPropertyNames()).containsExactly("username");
        assertThat(bve.getFieldMessages()).containsExactly("must not be blank");
    }

    // -------------------------------------------------------------------------
    // Cause chain: adapter found for a nested cause, not the wrapper
    // -------------------------------------------------------------------------

    @Test
    void nestedOptimisticLockCauseMapsToEntityLockException() {
        // ExceptionMapper unwinds the cause stack: most specific (deepest) first.
        // The top-level wrapper has no adapter; the nested cause does.
        OptimisticLockException cause = new OptimisticLockException("stale");
        RuntimeException wrapper = new RuntimeException("transaction failed", cause);

        RuntimeException result = mapper.convertException(wrapper);

        assertThat(result).isInstanceOf(EntityLockException.class);
    }
}
