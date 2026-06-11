/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
import java.sql.SQLException;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityExceptionAdapter;

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
	 * @see com.rreganjr.platform.exception.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Object,
	 *      com.rreganjr.platform.exception.EntityExceptionActionType)
	 */
	@Override
	public EntityException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
        if (entityType == null && entity == null && original instanceof ConstraintViolationException cve) {
            SQLException sqle = cve.getSQLException();
            // SQLState class "23" is the integrity-constraint-violation family — uniqueness AND
            // referential-integrity (FK) violations. The old check only matched the exact "23000",
            // so an H2 FK violation (e.g. SQLState 23506/23503) fell through to the generic
            // "unknown" label below, masking a real referential-integrity failure during #69
            // testing. Surface the actual constraint name + DB message for any 23xxx so the error
            // is self-explanatory.
            String sqlState = sqle != null ? sqle.getSQLState() : null;
            if (sqlState != null && sqlState.startsWith("23")) {
                String constraint = cve.getConstraintName();
                String detail = (constraint != null && !constraint.isBlank())
                        ? "constraint " + constraint + ": " + sqle.getMessage()
                        : sqle.getMessage();
                return EntityException.uniquenessConflict(sqle, detail);
            }
        }
        return EntityException.uniquenessConflict(entityType, entity, propertyName, actionType);
    }

}
