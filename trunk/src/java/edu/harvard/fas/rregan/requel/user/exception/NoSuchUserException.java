/*
 * $Id: NoSuchUserException.java,v 1.4 2008/12/13 00:41:36 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
