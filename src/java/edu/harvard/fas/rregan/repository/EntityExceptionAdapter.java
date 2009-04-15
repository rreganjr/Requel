/*
 * $Id: EntityExceptionAdapter.java,v 1.1 2008/12/13 00:40:43 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository;


/**
 * @author ron
 */
public interface EntityExceptionAdapter {

	/**
	 * Convert an API specific exception into an appropriate EntityException
	 * with an appropriate message to display to the user.
	 * 
	 * @param original -
	 *            the original API specific exception/throwable thrown.
	 * @param entityType -
	 *            the canonical type of Object (interface) to display.
	 * @param entity -
	 *            the entity that caused the exception
	 * @param actionType -
	 *            the type of action (create, read, update, delete) being done
	 *            when the exception happened.
	 * @return
	 */
	public EntityException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType);

}
