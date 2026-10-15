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
package com.rreganjr.validator;

/**
 * Minimal shim to support legacy validation error handling.
 * Provides the properties referenced by existing code when formatting
 * validation failure messages.
 */
public class InvalidValue {
    private final Class<?> beanClass;
    private final String propertyName;
    private final Object value;
    private final String message;

    public InvalidValue(String message, Class<?> beanClass, String propertyName, Object value) {
        this.message = message;
        this.beanClass = beanClass;
        this.propertyName = propertyName;
        this.value = value;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }
}

