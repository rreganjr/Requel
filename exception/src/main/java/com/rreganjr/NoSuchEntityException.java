/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr;

/**
 * @author ron
 */
public class NoSuchEntityException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_NO_SUCH_ENTITY_SINGLE_PROPERTY_SEARCH = "No %s found for %s of %s";
	protected static String MSG_NO_SUCH_ENTITY_MULTIPLE_PROPERTY_SEARCH = "No %s found for properties %s with values %s";

	public static NoSuchEntityException byQuery(Class<?> entityType, String entityPropertyName,
			Object entityPropertyValue) {
		return new NoSuchEntityException(entityType, null, entityPropertyName, entityPropertyValue,
				EntityExceptionActionType.Reading, MSG_NO_SUCH_ENTITY_SINGLE_PROPERTY_SEARCH,
				entityType.getSimpleName(), entityPropertyName, entityPropertyValue);
	}

	/**
	 * @param entityType
	 * @param entityPropertyNames
	 * @param entityPropertyValues
	 * @return
	 */
	public static NoSuchEntityException byQuery(Class<?> entityType, String[] entityPropertyNames,
			Object[] entityPropertyValues) {
		return new NoSuchEntityException(entityType, null, entityPropertyNames,
				entityPropertyValues, EntityExceptionActionType.Reading,
				MSG_NO_SUCH_ENTITY_MULTIPLE_PROPERTY_SEARCH, entityType.getSimpleName(),
				entityPropertyNames, entityPropertyValues);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchEntityException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityPropertyValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityPropertyValue, actionType, format,
				messageArgs);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchEntityException(Class<?> entityType, Object entity,
			String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyNames, entityPropertyValues, actionType, format,
				messageArgs);
	}

	protected NoSuchEntityException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityPropertyValue,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityPropertyValue, actionType,
				format, messageArgs);
	}

	protected NoSuchEntityException(Throwable cause, Class<?> entityType, Object entity,
			String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyNames, entityPropertyValues, actionType,
				format, messageArgs);
	}
}
