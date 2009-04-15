/*
 * $Id: UserEntityException.java,v 1.2 2008/12/13 00:41:36 rregan Exp $
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
