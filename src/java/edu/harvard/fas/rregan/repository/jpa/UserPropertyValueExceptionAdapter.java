/*
 * $Id: UserPropertyValueExceptionAdapter.java,v 1.2 2009/02/17 11:50:50 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository.jpa;

import org.hibernate.PropertyValueException;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.EntityExceptionAdapter;
import edu.harvard.fas.rregan.requel.EntityValidationException;

/**
 * @author ron
 */
public class UserPropertyValueExceptionAdapter implements EntityExceptionAdapter {

	/**
	 * @see edu.harvard.fas.rregan.repository.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Object,
	 *      edu.harvard.fas.rregan.repository.EntityExceptionActionType)
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
