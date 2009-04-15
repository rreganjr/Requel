/*
 * $Id: NoSuchUserException.java,v 1.4 2008/12/13 00:41:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
public class NoSuchUserException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_NO_USER_FOR_NAME = "No user for username '%s'";
	protected static String MSG_WRONG_PASSWORD = "The password for username '%s' was incorrect.";

	/**
	 * @param username
	 * @return
	 */
	public static NoSuchUserException forUsername(String username) {
		return new NoSuchUserException(User.class, null, "username", username,
				EntityExceptionActionType.Reading, MSG_NO_USER_FOR_NAME, username);
	}

	/**
	 * @param user
	 * @return
	 */
	public static NoSuchUserException wrongPasswordForUser(User user) {
		return new NoSuchUserException(User.class, user, "password", null,
				EntityExceptionActionType.Reading, MSG_WRONG_PASSWORD, user.getUsername());
	}

	protected NoSuchUserException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected NoSuchUserException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
