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
package com.rreganjr.requel.project.exception;

import com.rreganjr.EntityExceptionActionType;
import com.rreganjr.NoSuchEntityException;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ProjectOrDomain;

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