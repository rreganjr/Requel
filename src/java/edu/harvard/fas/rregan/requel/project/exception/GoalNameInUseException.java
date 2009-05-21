/*
 * $Id$
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
