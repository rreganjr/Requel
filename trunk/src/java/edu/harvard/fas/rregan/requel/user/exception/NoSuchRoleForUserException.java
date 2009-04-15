/*
 * $Id: NoSuchRoleForUserException.java,v 1.5 2008/12/13 00:41:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRole;

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
