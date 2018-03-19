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

import org.hibernate.exception.ConstraintViolationException;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import com.rreganjr.EntityException;
import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.repository.EntityExceptionAdapter;

/**
 * @author ron
 */
public class EntityExistsExceptionAdapter implements EntityExceptionAdapter {

	/**
	 */
	public EntityExistsExceptionAdapter() {
	}

	/**
	 * @see com.rreganjr.repository.EntityExceptionAdapter#convert(Throwable, Class, Object, EntityExceptionActionType)
	 */
	@Override
	public EntityException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {

		if (entityType == null) {
			if (entity == null) {
				if (ConstraintViolationException.class.equals(original.getCause().getClass())) {
					ConstraintViolationException cve = (ConstraintViolationException) original
							.getCause();
					if (MySQLIntegrityConstraintViolationException.class.equals(cve.getCause()
							.getClass())) {
						MySQLIntegrityConstraintViolationException icve = (MySQLIntegrityConstraintViolationException) cve
								.getCause();
						return EntityException.uniquenessConflict(icve, icve.getMessage());
					}
				}
			}
		}
		return EntityException.uniquenessConflict(entityType, entity, null, actionType);
	}

}
