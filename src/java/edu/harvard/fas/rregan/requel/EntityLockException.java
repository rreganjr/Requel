/*
 * $Id: EntityLockException.java,v 1.3 2008/12/18 02:01:54 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
				MSG_STALE_ENTITY, (entityType == null ? "Unknown Type":entityType.getSimpleName()));
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
