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
package edu.harvard.fas.rregan.nlp.impl;

import edu.harvard.fas.rregan.ApplicationException;
import edu.harvard.fas.rregan.nlp.NLPText;

/**
 * @author ron
 */
public class NLPProcessorException extends ApplicationException {
	static final long serialVersionUID = 0;

	protected static final String MSG_FORMAT_NO_PRIMARY_VERB = "No primary verb found in text '%s'";

	/**
	 * @param text -
	 *            The text being processed
	 * @return a NLPProcessorException with a message that the primary verb
	 *         wasn't found in the supplied NLPText.
	 */
	protected static NLPProcessorException noPrimaryVerb(NLPText text) {
		return new NLPProcessorException(MSG_FORMAT_NO_PRIMARY_VERB, text);
	}

	/**
	 * @param format
	 * @param args
	 */
	protected NLPProcessorException(String format, Object... args) {
		super(format, args);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected NLPProcessorException(Throwable cause, String format, Object... args) {
		super(cause, format, args);
	}

}