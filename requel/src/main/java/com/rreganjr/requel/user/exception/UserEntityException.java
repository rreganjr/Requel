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

import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRole;

/**
 * @author ron
 */
public class UserEntityException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_EXCEPTION_GRANTING_ROLE = "Exception granting role '%s' to user '%s': %s";

	protected static String MSG_EXCEPTION_REVOKING_ROLE = "Exception revoking role '%s' from user '%s': %s";

	/**
	 * @param userRoleType -
	 *            the type of role being granted
	 * @param user -
	 *            the user the role was being granted to
	 * @param cause -
	 *            the exception that occured during the grant
	 * @return
	 */
	public static UserEntityException exceptionGrantingRole(Class<? extends UserRole> userRoleType,
			User user, Exception cause) {
		return new UserEntityException(cause, User.class, user, "userRoles", userRoleType,
				EntityExceptionActionType.Creating, MSG_EXCEPTION_GRANTING_ROLE, userRoleType
						.getSimpleName(), user.getUsername(), cause);
	}

	/**
	 * @param userRoleType -
	 *            the type of role being revoked
	 * @param user -
	 *            the user the role was being revoked from
	 * @param cause -
	 *            the exception that occured during the revoke.
	 * @return
	 */
	public static UserEntityException exceptionRevokingRole(Class<? extends UserRole> userRoleType,
			User user, Exception cause) {
		return new UserEntityException(cause, User.class, user, "userRoles", userRoleType,
				EntityExceptionActionType.Creating, MSG_EXCEPTION_REVOKING_ROLE, userRoleType
						.getSimpleName(), user.getUsername(), cause);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected UserEntityException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected UserEntityException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
