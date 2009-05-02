/*
 * $Id: UIFrameworkException.java,v 1.2 2009/01/03 10:24:39 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework;

import edu.harvard.fas.rregan.ApplicationException;

/**
 * @author ron
 */
public class UIFrameworkException extends ApplicationException {
	static final long serialVersionUID = 0;

	protected static String MSG_COULD_NOT_CREATE_PANEL = "Could not create panel for class %s";
	protected static String MSG_UNSAVED_DATA = "The current panel has unsaved data.";
	protected static String MSG_EXCEPTION_IN_CONFIG = "The configuration encountered a problem during initialization: %s.";

	/**
	 * Create an exception indicating that the Echo2 stylesheet couldn't be
	 * loaded because of an error.
	 * 
	 * @param message -
	 *            The message from the stylesheet loader.
	 * @return
	 */
	public static ApplicationException errorInStyleSheet(String message) {
		return new UIFrameworkException(MSG_EXCEPTION_IN_CONFIG, message);
	}

	/**
	 * Create an exception indicating an unexpected exception during application
	 * configuration.
	 * 
	 * @param cause
	 * @return
	 */
	public static ApplicationException exceptionInConfig(Throwable cause) {
		return new UIFrameworkException(cause, MSG_EXCEPTION_IN_CONFIG, cause);
	}

	/**
	 * @param panelClass
	 * @param cause
	 * @return
	 */
	public static ApplicationException couldNotCreatePanel(Class<?> panelClass, Throwable cause) {
		return new UIFrameworkException(cause, MSG_COULD_NOT_CREATE_PANEL, panelClass.getName());
	}

	/**
	 * @return
	 */
	public static ApplicationException panelHasUnsavedData() {
		return new UIFrameworkException(MSG_UNSAVED_DATA);
	}

	/**
	 * @param format -
	 *            a format string appropriate for java.util.Formatter
	 * @param args -
	 *            variable args list that map to the variables in the format
	 *            string
	 */
	protected UIFrameworkException(String format, Object... args) {
		super(format, args);
	}

	/**
	 * @param cause -
	 *            a caught exception that resulted in this exception
	 * @param format -
	 *            a format string appropriate for java.util.Formatter
	 * @param args -
	 *            variable args list that map to the variables in the format
	 *            string
	 */
	protected UIFrameworkException(Throwable cause, String format, Object... args) {
		super(format, args, cause);
	}
}
