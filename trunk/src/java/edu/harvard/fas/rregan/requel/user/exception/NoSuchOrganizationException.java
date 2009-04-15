/*
 * $Id: NoSuchOrganizationException.java,v 1.3 2008/12/13 00:41:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.user.Organization;

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

	protected NoSuchOrganizationException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected NoSuchOrganizationException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
