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

import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.repository.EntityExceptionAdapter;
import com.rreganjr.EntityLockException;

/**
 * @author ron
 */
public class StaleObjectStateExceptionAdapter implements EntityExceptionAdapter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rreganjr.requel.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Class, java.lang.Object,
	 *      com.rreganjr.requel.EntityExceptionActionType)
	 */
	@Override
	public EntityLockException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		return EntityLockException.staleEntity(entityType, entity, actionType);
	}

}
