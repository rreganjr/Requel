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
package com.rreganjr.nlp;

import com.rreganjr.ApplicationException;

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
