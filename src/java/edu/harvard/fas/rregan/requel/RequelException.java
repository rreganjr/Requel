/*
 * $Id: RequelException.java,v 1.1 2008/03/14 10:26:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel;

import edu.harvard.fas.rregan.ApplicationException;

/**
 * @author ron
 */
public class RequelException extends ApplicationException {
	static final long serialVersionUID = 0;

	/**
	 * @param format
	 * @param args
	 */
	protected RequelException(String format, Object... args) {
		super(format, args);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected RequelException(Throwable cause, String format, Object... args) {
		super(cause, format, args);
	}
}
