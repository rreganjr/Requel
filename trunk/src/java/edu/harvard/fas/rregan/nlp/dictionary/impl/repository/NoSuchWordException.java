/*
 * $Id: NoSuchWordException.java,v 1.1 2008/12/15 06:36:02 rregan Exp $
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
