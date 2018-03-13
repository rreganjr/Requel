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
package com.rreganjr.repository;

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
