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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Extends EntityValidationException to carry per-field validation messages
 * from jakarta.validation.ConstraintViolationException.
 */
public class BeanValidationException extends EntityValidationException {
    static final long serialVersionUID = 0;

    /**
     * Orders violations by property path, then by message. Applied so that a bean with more than one
     * violation always reports its fields in the same order.
     *
     * <p>This has to happen <em>here</em>, on the way out of the
     * {@link ConstraintViolationException}, rather than in the caller on the way in:
     * {@code ConstraintViolationException}'s constructor copies the set it is given into a
     * {@code HashSet}, so any ordering a caller establishes beforehand is discarded before this
     * class ever sees it.
     */
    private static final Comparator<ConstraintViolation<?>> BY_PATH_THEN_MESSAGE =
            Comparator.comparing((ConstraintViolation<?> v) -> v.getPropertyPath().toString())
                    .thenComparing(ConstraintViolation::getMessage);

    private final String[] fieldMessages;

    /**
     * Build one from a {@link ConstraintViolationException}, flattening its violations into the
     * parallel {@code propertyNames} / {@code fieldMessages} arrays that
     * {@code CommandController} turns into {@code CommandResult.FieldViolation}s.
     *
     * <p>Shared by the two places bean validation surfaces: at Hibernate flush time via
     * {@link BeanValidationExceptionAdapter} (entity constraints), and before command construction
     * via {@code CommandInputValidator} (input DTO constraints, issue #171). Both need the exact
     * same shape, so the construction lives here rather than being duplicated at each site.
     *
     * <p>Violations are sorted (see {@link #BY_PATH_THEN_MESSAGE}), because
     * {@code Validator.validate} returns an unordered {@code Set} and a bean with two violations
     * would otherwise report its fields in an arbitrary order — a combined message that changes
     * between runs, and flaky assertions in any test that names more than one field.
     */
    public static BeanValidationException of(ConstraintViolationException cve, Class<?> entityType,
            Object entity, EntityExceptionActionType actionType) {
        List<ConstraintViolation<?>> violations = new ArrayList<>(cve.getConstraintViolations());
        violations.sort(BY_PATH_THEN_MESSAGE);
        String[] propertyNames = new String[violations.size()];
        String[] messages = new String[violations.size()];
        for (int i = 0; i < violations.size(); i++) {
            propertyNames[i] = violations.get(i).getPropertyPath().toString();
            messages[i] = violations.get(i).getMessage();
        }
        StringBuilder msg = new StringBuilder();
        for (int j = 0; j < propertyNames.length; j++) {
            if (j > 0) {
                msg.append("; ");
            }
            msg.append(propertyNames[j]).append(": ").append(messages[j]);
        }
        return new BeanValidationException(cve, entityType, entity, propertyNames, messages,
                actionType, msg.toString());
    }

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
