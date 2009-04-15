/*
 * $Id: EntityValidationException.java,v 1.5 2009/02/17 11:50:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel;

import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;

/**
 * @author ron
 */
public class EntityValidationException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_VALIDATION_FAILED = "validation failed: %s";
	protected static String MSG_EMPTY_VALUE = "%s cannot be empty.";

	/**
	 * Create an EntityException for an empty property that is required.
	 * 
	 * @param actionType
	 * @param entityType
	 * @param entity
	 * @param propertyName
	 * @return a EntityException
	 */
	public static EntityValidationException emptyRequiredProperty(Class<?> entityType,
			Object entity, String propertyName, EntityExceptionActionType actionType) {
		return new EntityValidationException(entityType, entity, propertyName, null, actionType,
				MSG_EMPTY_VALUE, propertyName);
	}

	/**
	 * @param entityType
	 * @param propertyName
	 * @param message
	 * @return
	 */
	public static EntityValidationException validationFailed(Class<?> entityType,
			String propertyName, String message) {
		return new EntityValidationException(entityType, null, propertyName, null,
				EntityExceptionActionType.Unknown, MSG_VALIDATION_FAILED, message);
	}

	/**
	 * @param cause
	 * @param entityType
	 * @param entity
	 * @param actionType
	 * @return
	 */
	public static EntityValidationException validationFailed(InvalidStateException cause,
			Class<?> entityType, Object entity, EntityExceptionActionType actionType) {
		StringBuilder causeMsg = new StringBuilder();
		for (InvalidValue value : cause.getInvalidValues()) {
			causeMsg.append(System.getProperty("line.separator"));
			causeMsg.append(value.getBeanClass().getSimpleName());
			causeMsg.append(" ");
			causeMsg.append(value.getPropertyName());
			causeMsg.append(" ");
			causeMsg.append(value.getValue());
			causeMsg.append(": ");
			causeMsg.append(value.getMessage());
		}
		return new EntityValidationException(cause, entityType, entity, null, null, actionType,
				MSG_VALIDATION_FAILED, causeMsg);
	}

	/**
	 * @param entityType
	 * @param entity
	 * @param entityPropertyName
	 * @param entityValue
	 * @param actionType
	 * @param format
	 * @param messageArgs
	 */
	public EntityValidationException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param entityType
	 * @param entity
	 * @param entityPropertyName
	 * @param entityValue
	 * @param actionType
	 * @param format
	 * @param messageArgs
	 */
	public EntityValidationException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}

	@Override
	public InvalidStateException getCause() {
		return (InvalidStateException) super.getCause();
	}
}
