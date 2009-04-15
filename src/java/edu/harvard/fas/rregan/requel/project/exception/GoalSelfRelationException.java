/*
 * $Id: GoalSelfRelationException.java,v 1.1 2009/03/26 00:43:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalRelation;

/**
 * @author ron
 */
public class GoalSelfRelationException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG = "A goal cannot have a relation to itself.";

	/**
	 * @param goal -
	 *            the goal
	 * @return
	 */
	public static GoalSelfRelationException forGoal(Goal goal) {
		return new GoalSelfRelationException(GoalRelation.class, null, "toGoal", goal,
				EntityExceptionActionType.Creating, MSG);
	}

	protected GoalSelfRelationException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	protected GoalSelfRelationException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
