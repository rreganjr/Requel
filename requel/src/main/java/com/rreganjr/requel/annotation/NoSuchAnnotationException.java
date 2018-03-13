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
package com.rreganjr.requel.annotation;

import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.requel.NoSuchEntityException;

/**
 * @author ron
 */
public class NoSuchAnnotationException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_MESSAGE = "No annotation exists with message '%s'";
	protected static String MSG_FOR_LEXICAL_ISSUE = "No lexical issue exists for word '%s'";

	/**
	 * @param message -
	 *            the message used to search for an issue that didn't exist
	 * @return
	 */
	public static NoSuchAnnotationException forMessage(String message) {
		return new NoSuchAnnotationException(Annotation.class, null, "message", message,
				EntityExceptionActionType.Reading, MSG_FOR_MESSAGE, message);
	}

	/**
	 * @param word -
	 *            the word used to search for an issue that didn't exist
	 * @param annotatableEntityPropertyName -
	 *            the property of the entity in question.
	 * @return
	 */
	public static NoSuchAnnotationException forWord(String word,
			String annotatableEntityPropertyName) {
		return new NoSuchAnnotationException(Issue.class, null, "word", word,
				EntityExceptionActionType.Reading, MSG_FOR_LEXICAL_ISSUE, word,
				annotatableEntityPropertyName);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchAnnotationException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchAnnotationException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
