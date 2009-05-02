/*
 * $Id: EventDispatcherException.java,v 1.2 2008/02/16 22:42:40 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.navigation.event;

import edu.harvard.fas.rregan.uiframework.UIFrameworkException;

/**
 * @author ron
 */
public class EventDispatcherException extends UIFrameworkException {
	static final long serialVersionUID = 0L;

	public static String MSG_ILLEGAL_EVENT_SOURCE_TYPE = "The event source %s is a class that isn't a Command.";
	public static String MSG_ILLEGAL_EVENT_SOURCE_INSTANCE = "The event source %s of type %s has no addActionListener method to register ActionListeners.";
	public static String MSG_PROBLEM_REGISTERING_LISTENER = "couldn't register EventDispatcher as listener for event source %s, addActionListener threw an exception: %s";
	public static String MSG_ILLEGAL_EVENT_TYPE = "The eventType %s is not an ActionEvent class or String.";

	public static EventDispatcherException illegalEventSourceType(Class<?> eventSourceType) {
		return new EventDispatcherException(MSG_ILLEGAL_EVENT_SOURCE_TYPE, eventSourceType);
	}

	public static EventDispatcherException illegalEventSourceObject(Object eventSource) {
		String eventSourceTypeName = (eventSource instanceof Class ? ((Class<?>) eventSource)
				.getName() : eventSource.getClass().getName());
		return new EventDispatcherException(MSG_ILLEGAL_EVENT_SOURCE_INSTANCE, eventSource,
				eventSourceTypeName);
	}

	public static EventDispatcherException problemRegisteringListenerToSource(Object eventSource,
			Throwable t) {
		return new EventDispatcherException(MSG_PROBLEM_REGISTERING_LISTENER, eventSource, t);
	}

	public static EventDispatcherException illegalEventType(Object eventType) {
		String eventTypeName = (eventType instanceof Class ? ((Class<?>) eventType).getName()
				: eventType.getClass().getName());
		return new EventDispatcherException(MSG_ILLEGAL_EVENT_TYPE, eventTypeName);
	}

	/**
	 * @param format -
	 *            a format string appropriate for java.util.Formatter
	 * @param args -
	 *            variable args list that map to the variables in the format
	 *            string
	 */
	protected EventDispatcherException(String format, Object... args) {
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
	protected EventDispatcherException(Throwable cause, String format, Object... args) {
		super(cause, format, args);
	}
}
