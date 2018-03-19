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

import com.rreganjr.EntityException;
import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRole;

/**
 * @author ron
 */
public class UserRoleExistsException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "The user role '%s' already exists.";
	protected static String MSG_FOR_NAME_AND_USER = "The user role '%s' for user '%s' already exists.";

	/**
	 * @param userRole -
	 *            the userRole that already exists.
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static UserRoleExistsException forRole(UserRole userRole) {
		return new UserRoleExistsException(UserRole.class, userRole, null, null,
				EntityExceptionActionType.Creating, MSG_FOR_NAME, userRole.getRoleName());
	}

	/**
	 * @param userRole -
	 *            the userRole that already exists for the supplied user.
	 * @param user -
	 *            the user that the role already exists for.
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static UserRoleExistsException forRoleAndUser(UserRole userRole, User user) {
		return new UserRoleExistsException(User.class, user, "userRoles", userRole,
				EntityExceptionActionType.Updating, MSG_FOR_NAME_AND_USER, userRole.getRoleName(),
				user.getUsername());
	}

	/**
	 * @param format
	 * @param args
	 */
	protected UserRoleExistsException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected UserRoleExistsException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
