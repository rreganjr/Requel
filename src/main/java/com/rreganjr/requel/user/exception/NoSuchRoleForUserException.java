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
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRole;

/**
 * @author ron
 */
public class NoSuchRoleForUserException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_NO_ROLE_FOR_USER = "The user '%s' does not have the role '%s'";

	/**
	 * @param user -
	 *            the user that doesn't have the role.
	 * @param userRoleType -
	 *            the type of the role that the user doesn't have.
	 * @return
	 */
	public static NoSuchRoleForUserException forUserRoleTypeName(User user,
			Class<? extends UserRole> userRoleType) {
		return new NoSuchRoleForUserException(User.class, user, "userRoles", userRoleType,
				EntityExceptionActionType.Reading, MSG_NO_ROLE_FOR_USER, user.getUsername(),
				userRoleType.getSimpleName());
	}

	protected NoSuchRoleForUserException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected NoSuchRoleForUserException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
