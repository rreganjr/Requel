/*
 * $Id: AnnotationExistsException.java,v 1.2 2008/12/13 00:41:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;

/**
 * @author ron
 */
public class AnnotationExistsException extends EntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_MESSAGE = "An annotation already exists with message '%s'";
	protected static String MSG_FOR_LEXICAL_ISSUE = "A lexical issue already exists for word '%s'";

	/**
	 * @param message -
	 *            the message used to search for an issue that already exists
	 * @return
	 */
	public static AnnotationExistsException forMessage(String message) {
		return new AnnotationExistsException(Annotation.class, null, "message", message,
				EntityExceptionActionType.Reading, MSG_FOR_MESSAGE, message);
	}

	/**
	 * @param word -
	 *            the word used to search for an issue that already exists
	 * @param annotatableEntityPropertyName -
	 *            the property of the entity in question.
	 * @return
	 */
	public static AnnotationExistsException forWord(String word,
			String annotatableEntityPropertyName) {
		return new AnnotationExistsException(Issue.class, null, "word", word,
				EntityExceptionActionType.Reading, MSG_FOR_LEXICAL_ISSUE, word,
				annotatableEntityPropertyName);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected AnnotationExistsException(Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected AnnotationExistsException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
