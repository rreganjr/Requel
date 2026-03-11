package com.rreganjr.repository.jpa;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityExceptionAdapter;
import com.rreganjr.validator.EntityValidationException;

/**
 * Converts jakarta.validation.ConstraintViolationException (Bean Validation)
 * into EntityValidationException so that the command handler chain surfaces
 * validation errors instead of opaque transaction failures.
 */
public class BeanValidationExceptionAdapter implements EntityExceptionAdapter {

    @Override
    public EntityException convert(Throwable original, Class<?> entityType, Object entity,
            EntityExceptionActionType actionType) {
        ConstraintViolationException cve = (ConstraintViolationException) original;
        var violations = cve.getConstraintViolations();
        String[] propertyNames = new String[violations.size()];
        String[] messages = new String[violations.size()];
        int i = 0;
        for (ConstraintViolation<?> v : violations) {
            propertyNames[i] = v.getPropertyPath().toString();
            messages[i] = v.getMessage();
            i++;
        }
        // Build a combined message for the exception
        StringBuilder msg = new StringBuilder();
        for (int j = 0; j < propertyNames.length; j++) {
            if (j > 0) msg.append("; ");
            msg.append(propertyNames[j]).append(": ").append(messages[j]);
        }
        return new BeanValidationException(cve, entityType, entity, propertyNames, messages, actionType, msg.toString());
    }
}
