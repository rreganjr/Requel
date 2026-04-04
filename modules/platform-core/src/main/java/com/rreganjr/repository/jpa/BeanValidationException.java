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

import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;

/**
 * Extends EntityValidationException to carry per-field validation messages
 * from jakarta.validation.ConstraintViolationException.
 */
public class BeanValidationException extends EntityValidationException {
    static final long serialVersionUID = 0;

    private final String[] fieldMessages;

    public BeanValidationException(Throwable cause, Class<?> entityType, Object entity,
            String[] propertyNames, String[] fieldMessages,
            EntityExceptionActionType actionType, String combinedMessage) {
        super(entityType, entity, propertyNames, null, actionType,
                MSG_VALIDATION_FAILED, combinedMessage);
        this.fieldMessages = fieldMessages;
        initCause(cause);
    }

    /**
     * Per-field messages, parallel to getEntityPropertyNames().
     */
    public String[] getFieldMessages() {
        return fieldMessages;
    }
}
