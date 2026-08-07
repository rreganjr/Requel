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
package com.rreganjr.repository.jpa;

import jakarta.validation.ConstraintViolationException;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityExceptionAdapter;

/**
 * Converts jakarta.validation.ConstraintViolationException (Bean Validation)
 * into EntityValidationException so that the command handler chain surfaces
 * validation errors instead of opaque transaction failures.
 *
 * <p>This is the flush-time path, for constraints declared on JPA entities. Constraints declared on
 * command input DTOs are validated earlier, by {@code CommandInputValidator} (issue #171), which
 * produces the same exception via {@link BeanValidationException#of}.
 */
public class BeanValidationExceptionAdapter implements EntityExceptionAdapter {

    @Override
    public EntityException convert(Throwable original, Class<?> entityType, Object entity,
            EntityExceptionActionType actionType) {
        return BeanValidationException.of((ConstraintViolationException) original, entityType,
                entity, actionType);
    }
}
