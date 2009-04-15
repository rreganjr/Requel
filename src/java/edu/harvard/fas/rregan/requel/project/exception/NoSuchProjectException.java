/*
 * $Id: NoSuchProjectException.java,v 1.4 2008/12/13 00:40:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.project.Project;

/**
 * @author ron
 */
public class NoSuchProjectException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "No project exists for name '%s'";

	/**
	 * @param name -
	 *            the name used to find a project that doesn't exist
	 * @return
	 */
	public static NoSuchProjectException forName(String name) {
		return new NoSuchProjectException(Project.class, null, "name", name,
				EntityExceptionActionType.Reading, MSG_FOR_NAME, name);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchProjectException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchProjectException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
