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

import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.NoSuchEntityException;
import com.rreganjr.requel.user.Organization;

/**
 * @author ron
 */
public class NoSuchOrganizationException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_NO_ORGANIZATION_FOR_NAME = "No organzation for name '%s'";

	/**
	 * @param username
	 * @return
	 */
	public static NoSuchOrganizationException forName(String name) {
		return new NoSuchOrganizationException(Organization.class, null, "name", name,
				EntityExceptionActionType.Reading, MSG_NO_ORGANIZATION_FOR_NAME, name);
	}

	protected NoSuchOrganizationException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected NoSuchOrganizationException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
