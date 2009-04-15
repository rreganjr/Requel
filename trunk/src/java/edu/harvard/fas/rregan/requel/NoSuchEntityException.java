/*
 * $Id: NoSuchEntityException.java,v 1.2 2008/12/13 00:41:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;

/**
 * @author ron
 */
public class NoSuchEntityException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_NO_SUCH_ENTITY_SINGLE_PROPERTY_SEARCH = "No %s found for %s of %s";
	protected static String MSG_NO_SUCH_ENTITY_MULTIPLE_PROPERTY_SEARCH = "No %s found for properties %s with values %s";
	

	public static NoSuchEntityException byQuery(Class<?> entityType,
			String entityPropertyName, Object entityPropertyValue) {
		return new NoSuchEntityException(entityType, null, entityPropertyName, entityPropertyValue, EntityExceptionActionType.Reading,
				MSG_NO_SUCH_ENTITY_SINGLE_PROPERTY_SEARCH, entityType.getSimpleName(), entityPropertyName, entityPropertyValue);
	}

	/**
	 * 
	 * @param entityType
	 * @param entityPropertyNames
	 * @param entityPropertyValues
	 * @return
	 */
	public static NoSuchEntityException byQuery(Class<?> entityType,
			String[] entityPropertyNames, Object[] entityPropertyValues) {
		return new NoSuchEntityException(entityType, null, entityPropertyNames, entityPropertyValues, EntityExceptionActionType.Reading,
				MSG_NO_SUCH_ENTITY_MULTIPLE_PROPERTY_SEARCH, entityType.getSimpleName(), entityPropertyNames, entityPropertyValues);
	}

	
	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchEntityException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityPropertyValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityPropertyValue, actionType, format, messageArgs);
	}
	
	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchEntityException(Class<?> entityType, Object entity, String[] entityPropertyNames,
			Object[] entityPropertyValues, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyNames, entityPropertyValues, actionType, format, messageArgs);
	}

	protected NoSuchEntityException(Throwable cause, Class<?> entityType, Object entity,
			 String entityPropertyName, Object entityPropertyValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityPropertyValue, actionType, format,
				messageArgs);
	}

	protected NoSuchEntityException(Throwable cause, Class<?> entityType, Object entity,
			 String[] entityPropertyNames, Object[] entityPropertyValues, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyNames, entityPropertyValues, actionType, format,
				messageArgs);
	}
}
