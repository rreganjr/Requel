/*
 * $Id: NoSuchActorException.java,v 1.3 2008/12/13 00:40:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.exception;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * @author ron
 */
public class NoSuchActorException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_NAME = "No actor for %s with name '%s'";

	/**
	 * @param projectOrDomain
	 * @param name -
	 *            the unknown word text.
	 * @return
	 */
	public static NoSuchActorException forProjectOrDomainWithName(ProjectOrDomain projectOrDomain,
			String name) {
		return new NoSuchActorException(Actor.class, null, "name", name,
				EntityExceptionActionType.Reading, MSG_FOR_NAME, projectOrDomain, name);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchActorException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchActorException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
