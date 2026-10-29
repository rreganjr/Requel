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
package com.rreganjr.requel.service.api;

import java.lang.reflect.RecordComponent;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates {@link RequiredOnCreate} on a record (issue #331). */
public class RequiredOnCreateValidator implements ConstraintValidator<RequiredOnCreate, Record> {

    private String idField;
    private String[] fields;

    @Override
    public void initialize(RequiredOnCreate annotation) {
        idField = annotation.id();
        fields = annotation.fields();
    }

    @Override
    public boolean isValid(Record input, ConstraintValidatorContext context) {
        if (input == null) {
            return true;
        }
        boolean creating = read(input, idField) == null;
        boolean valid = true;
        context.disableDefaultConstraintViolation();
        for (String field : fields) {
            Object value = read(input, field);
            boolean missing = value == null;
            boolean blank = value instanceof CharSequence text && text.toString().isBlank();
            if ((creating && (missing || blank)) || (!creating && blank)) {
                valid = false;
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(field)
                        .addConstraintViolation();
            }
        }
        return valid;
    }

    private static Object read(Record input, String component) {
        for (RecordComponent rc : input.getClass().getRecordComponents()) {
            if (rc.getName().equals(component)) {
                try {
                    return rc.getAccessor().invoke(input);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read " + component + " of "
                            + input.getClass().getSimpleName(), e);
                }
            }
        }
        throw new IllegalStateException("@RequiredOnCreate on " + input.getClass().getSimpleName()
                + " names a component it does not have: " + component);
    }
}
