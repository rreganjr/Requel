/*
 * $Id: EntityExistsExceptionAdapter.java,v 1.1 2008/12/13 00:41:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository.jpa;

import org.hibernate.exception.ConstraintViolationException;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.EntityExceptionAdapter;

/**
 * @author ron
 */
public class EntityExistsExceptionAdapter implements EntityExceptionAdapter {

	/**
	 * @param propertyName -
	 *            name of the property that must be unique.
	 */
	public EntityExistsExceptionAdapter() {
	}

	/**
	 * @see edu.harvard.fas.rregan.repository.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Object,
	 *      edu.harvard.fas.rregan.repository.EntityExceptionActionType)
	 */
	@Override
	public EntityException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		
		if (entityType == null) {
			if (entity == null ) {
				if (ConstraintViolationException.class.equals(original.getCause().getClass())) {
					ConstraintViolationException cve = (ConstraintViolationException) original.getCause();
					if (MySQLIntegrityConstraintViolationException.class.equals(cve.getCause().getClass())) {
						MySQLIntegrityConstraintViolationException icve = (MySQLIntegrityConstraintViolationException) cve.getCause();
						return EntityException.uniquenessConflict(icve, icve.getMessage());
					}
				}
			}
		}
		return EntityException.uniquenessConflict(entityType, entity, null, actionType);
	}

}
