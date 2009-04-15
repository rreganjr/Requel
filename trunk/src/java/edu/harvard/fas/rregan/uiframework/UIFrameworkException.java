/*
 * $Id: UIFrameworkException.java,v 1.2 2009/01/03 10:24:39 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
