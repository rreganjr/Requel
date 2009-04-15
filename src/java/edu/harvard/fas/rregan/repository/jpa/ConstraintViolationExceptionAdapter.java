/*
 * $Id: ConstraintViolationExceptionAdapter.java,v 1.1 2008/12/13 00:41:15 rregan Exp $
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
public class ConstraintViolationExceptionAdapter implements EntityExceptionAdapter {

	private final String propertyName;

	/**
	 * @param propertyName -
	 *            name of the property that must be unique.
	 */
	public ConstraintViolationExceptionAdapter(String propertyName) {
		this.propertyName = propertyName;
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
				ConstraintViolationException cve = (ConstraintViolationException) original;
				if (MySQLIntegrityConstraintViolationException.class.equals(cve.getCause().getClass())) {
					MySQLIntegrityConstraintViolationException icve = (MySQLIntegrityConstraintViolationException) cve.getCause();
					return EntityException.uniquenessConflict(icve, icve.getMessage());
				}
			}
		}
		return EntityException.uniquenessConflict(entityType, entity, propertyName, actionType);
	}

}
