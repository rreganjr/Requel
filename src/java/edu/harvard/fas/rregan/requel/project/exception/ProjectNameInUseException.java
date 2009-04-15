/*
 * $Id: ProjectNameInUseException.java,v 1.3 2008/12/13 00:40:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.project.Project;

/**
 * @author ron
 */
public class ProjectNameInUseException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "A project already exists with name '%s'";

	/**
	 * @param name -
	 *            the name used to find a project that doesn't exist
	 * @return
	 */
	public static ProjectNameInUseException forName(String name) {
		return new ProjectNameInUseException(Project.class, null, "name", name,
				EntityExceptionActionType.Creating, MSG_FOR_NAME, name);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected ProjectNameInUseException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected ProjectNameInUseException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
