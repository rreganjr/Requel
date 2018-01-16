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
package com.rreganjr.repository.jpa;

import org.hibernate.PropertyValueException;

import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.repository.EntityExceptionAdapter;
import com.rreganjr.requel.EntityValidationException;

/**
 * @author ron
 */
public class UserPropertyValueExceptionAdapter implements EntityExceptionAdapter {

	/**
	 * @see com.rreganjr.repository.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Object,
	 *      com.rreganjr.repository.EntityExceptionActionType)
	 */
	@Override
	public EntityException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		PropertyValueException pve = (PropertyValueException) original;
		String propertyName = pve.getPropertyName();
		if ("hashedPassword".equals(propertyName)) {
			propertyName = "password";
		}
		if (original.getMessage().startsWith(
				"not-null property references a null or transient value")) {
			return EntityValidationException.emptyRequiredProperty(entityType, entity,
					propertyName, actionType);
		} else {
			return EntityException.forUnknownProblem(original, entityType, entity, propertyName,
					null, actionType);
		}
	}
}
