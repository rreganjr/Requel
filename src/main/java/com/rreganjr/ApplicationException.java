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
package com.rreganjr;

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
	 * @return ApplicationException with message {@link #MSG_NOT_IMPLEMENTED}
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
		super(new Formatter().format(format, pretty(args)).toString());
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
		super(new Formatter().format(format, pretty(args)).toString(), cause);
		if (log.isDebugEnabled()) {
			log.debug(getMessage(), cause);
		}
	}
	
	static private Object[] pretty(Object[] args) {
		Object[] pretty = new Object[args.length];
		int i = 0;
		for (Object o : args) {
			if (o != null) {
				if (Object[].class.isAssignableFrom(o.getClass())) {
					StringBuilder b = new StringBuilder();
					Object[] inner = (Object[])args[i];
					for (int x = 0; x < inner.length - 1; x++) {
						b.append(pretty(inner[x]));
						b.append(", ");
					}
					b.append(pretty(inner[inner.length -1]));
					pretty[i] = b.toString();
				} else {
					pretty[i] = o.toString();
				}
			} else {
				pretty[i] = "<null>";
			}
			i++;
		}
		return pretty;
	}
	
	static private String pretty(Object o) {
		if (o == null) {
			return "<null>";
		} else if (o instanceof String) {
			return "\"" + o + "\"";
		} else {
			return o.toString();
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (getClass().equals(obj.getClass()) && this.toString().equals(obj.toString()));
	}
}
