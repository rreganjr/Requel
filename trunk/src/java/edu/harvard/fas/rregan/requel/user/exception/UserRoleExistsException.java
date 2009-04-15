/*
 * $Id: UserRoleExistsException.java,v 1.4 2008/12/13 00:41:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.exception;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRole;

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
