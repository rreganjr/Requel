/*
 * $Id: RequelException.java,v 1.1 2008/03/14 10:26:57 rregan Exp $
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
