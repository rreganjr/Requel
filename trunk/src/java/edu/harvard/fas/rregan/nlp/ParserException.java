/*
 * $Id: ParserException.java,v 1.3 2009/01/26 10:19:04 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp;

import edu.harvard.fas.rregan.ApplicationException;

/**
 * @author ron
 */
public class ParserException extends ApplicationException {
	static final long serialVersionUID = 0;

	public static final String MSG_FORMAT_PARSE_FAILED = "Parse failed.";

	/**
	 * @return a ParseException with a message that parsing failed.
	 */
	public static ParserException parseFailed() {
		return new ParserException(MSG_FORMAT_PARSE_FAILED);
	}

	protected ParserException(String format, Object... args) {
		super(format, args);
	}

	protected ParserException(Throwable e, String format, Object... args) {
		super(e, format, args);
	}

	protected ParserException(String msg) {
		super(msg);
	}

}
