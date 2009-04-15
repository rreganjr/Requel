/*
 * $Id: UsernameInUseException.java,v 1.4 2008/12/13 00:41:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.user.User;

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
