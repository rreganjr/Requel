/*
 * $Id: GoalNameInUseException.java,v 1.3 2008/12/13 00:40:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.Project;

/**
 * @author ron
 */
public class GoalNameInUseException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "A goal already exists with name '%s' for project '%s'";

	/**
	 * @param project -
	 *            the project the goal name already exists in
	 * @param name -
	 *            the name used to find a project that doesn't exist
	 * @return
	 */
	public static GoalNameInUseException forName(Project project, String name) {
		return new GoalNameInUseException(Goal.class, null, "name", name,
				EntityExceptionActionType.Creating, MSG_FOR_NAME, name, project.getName());
	}

	protected GoalNameInUseException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected GoalNameInUseException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
