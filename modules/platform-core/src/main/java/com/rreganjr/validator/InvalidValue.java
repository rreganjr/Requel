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

