/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;

/**
 * @author ron
 */
public class EntityLockException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_STALE_ENTITY = "The %s is out of date.";
	protected static String MSG_LOCK_ENTITY = "failed to aquire lock.";

	/**
	 * @param entityType
	 * @param entity
	 * @param actionType
	 * @return
	 */
	public static EntityLockException staleEntity(Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		return new EntityLockException(entityType, entity, null, null, actionType,
				MSG_STALE_ENTITY,
				(entityType == null ? "Unknown Type" : entityType.getSimpleName()));
	}

	/**
	 * @param entityType
	 * @param entity
	 * @param actionType
	 * @return
	 */
	public static EntityLockException deadlock(Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		return new EntityLockException(entityType, entity, null, null, actionType, MSG_LOCK_ENTITY,
				entityType.getSimpleName());
	}

	/**
	 * @param format
	 * @param args
	 */
	protected EntityLockException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected EntityLockException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
