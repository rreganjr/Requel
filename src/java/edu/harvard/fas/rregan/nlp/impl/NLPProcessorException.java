/*
 * $Id: NLPProcessorException.java,v 1.1 2009/02/10 10:26:04 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
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