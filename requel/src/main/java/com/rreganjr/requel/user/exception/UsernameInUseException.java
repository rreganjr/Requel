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
package com.rreganjr.requel.user.exception;

import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.user.User;

/**
 * @author ron
 */
public class UsernameInUseException extends UserEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "A user already exists with username '%s'";

	/**
	 * @param username -
	 *            the username already in use
	 * @return
	 */
	public static UsernameInUseException forName(String username) {
		return new UsernameInUseException(User.class, null, "username", username,
				EntityExceptionActionType.Creating, MSG_FOR_NAME, username);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected UsernameInUseException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected UsernameInUseException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
