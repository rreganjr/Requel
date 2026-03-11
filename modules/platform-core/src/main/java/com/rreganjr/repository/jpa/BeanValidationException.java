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
