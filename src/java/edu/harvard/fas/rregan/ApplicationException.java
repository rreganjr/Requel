/*
 * $Id: ApplicationException.java,v 1.7 2009/01/03 10:24:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan;

import java.util.Formatter;

import org.apache.log4j.Logger;

/**
 * @author ron
 */
public class ApplicationException extends RuntimeException {
	private static final Logger log = Logger.getLogger(ApplicationException.class);
	static final long serialVersionUID = 0;

	protected static String MSG_NOT_IMPLEMENTED = "Not implemented.";
	protected static String MSG_FAILED_TO_INITIALIZE_COMPONENT = "Failed to initialize the %s component: %s";
	protected static String MSG_UNSUPPORTED_DATE = "The supplied date '%s' is not in a recognized format.";
	protected static String MSG_PRE_GENERATED = "%s";
	protected static String MSG_MISSING_RESOURCE_BUNDLE = "The resource bundle '%s' could not be loaded: %s.";
	protected static String MSG_NO_RESOURCE_BUNDLE = "No resource bundle was supplied.";

	/**
	 * @param name -
	 *            the name used to find a project that doesn't exist
	 * @return
	 */
	public static ApplicationException notImplemented() {
		return new ApplicationException(MSG_NOT_IMPLEMENTED);
	}

	/**
	 * @param type -
	 *            the class that failed to initialize
	 * @param cause -
	 *            the reason the initialization failed
	 * @return
	 */
	public static ApplicationException failedToInitializeComponent(Class<?> type, Throwable cause) {
		return new ApplicationException(MSG_FAILED_TO_INITIALIZE_COMPONENT, type.getName(), cause
				.toString());
	}

	/**
	 * @param type -
	 *            the class that failed to initialize
	 * @param details -
	 *            a detailed message if an exception wasn't the cause of the
	 *            failure.
	 * @return
	 */
	public static ApplicationException failedToInitializeComponent(Class<?> type, String details) {
		return new ApplicationException(MSG_FAILED_TO_INITIALIZE_COMPONENT, type.getName(), details);
	}

	/**
	 * @param dateString
	 * @return
	 */
	public static ApplicationException unsupportedDateString(String dateString) {
		return new ApplicationException(MSG_UNSUPPORTED_DATE, dateString);
	}

	/**
	 * @param bundleName
	 * @param cause
	 * @return
	 */
	public static ApplicationException missingResourceBundle(String bundleName, Throwable cause) {
		return new ApplicationException(cause, MSG_MISSING_RESOURCE_BUNDLE, bundleName, cause);
	}

	/**
	 * @return
	 */
	public static ApplicationException missingResourceBundle() {
		return new ApplicationException(MSG_NO_RESOURCE_BUNDLE);
	}

	/**
	 * @param format -
	 *            a format string appropriate for java.util.Formatter
	 * @param args -
	 *            variable args list that map to the variables in the format
	 *            string
	 */
	protected ApplicationException(String format, Object... args) {
		super(new Formatter().format(format, args).toString());
		if (log.isDebugEnabled()) {
			log.debug(getMessage());
		}
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
	protected ApplicationException(Throwable cause, String format, Object... args) {
		super(new Formatter().format(format, args).toString(), cause);
		if (log.isDebugEnabled()) {
			log.debug(new Formatter().format(format, args).toString(), cause);
		}
	}
}
