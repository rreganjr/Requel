/*
 * $Id: NoSuchAnnotationException.java,v 1.3 2008/12/13 00:41:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.annotation;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;

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
	public static NoSuchAnnotationException forWord(String word, String annotatableEntityPropertyName) {
		return new NoSuchAnnotationException(Issue.class, null, "word", word,
				EntityExceptionActionType.Reading, MSG_FOR_LEXICAL_ISSUE, word,
				annotatableEntityPropertyName);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchAnnotationException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
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
