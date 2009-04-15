/*
 * $Id: NoSuchWordException.java,v 1.1 2008/12/15 06:36:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.impl.repository;

import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;

/**
 * @author ron
 */
public class NoSuchWordException extends NoSuchEntityException {
	static final long serialVersionUID = 0;

	protected static String MSG_FOR_TEXT = "Unknown word '%s'";
	protected static String MSG_FOR_TEXT_AND_POS = "Unknown word '%s' with part of speech '%s'";

	/**
	 * @param text -
	 *            the unknown word text.
	 * @return
	 */
	public static NoSuchWordException forLemma(String text) {
		return new NoSuchWordException(Word.class, null, "text", text,
				EntityExceptionActionType.Reading, MSG_FOR_TEXT, text);
	}

	/**
	 * @param text -
	 *            the unknown word text.
	 * @param pos -
	 *            the part of speech.
	 * @return
	 */
	public static NoSuchWordException forLemmaAndPOS(String text, String pos) {
		return new NoSuchWordException(Word.class, null, "text", text,
				EntityExceptionActionType.Reading, MSG_FOR_TEXT, text, pos);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NoSuchWordException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		super(entityType, entity, entityPropertyName, entityValue, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NoSuchWordException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityValue, EntityExceptionActionType actionType,
			String format, Object... messageArgs) {
		super(cause, entityType, entity, entityPropertyName, entityValue, actionType, format,
				messageArgs);
	}
}
