/*
 * $Id: EventDispatcherMultiException.java,v 1.2 2008/02/22 21:51:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.event;

import java.util.Map;

import nextapp.echo2.app.event.ActionListener;

public class EventDispatcherMultiException extends EventDispatcherException {
	static final long serialVersionUID = 0L;

	private final Map<ActionListener, Exception> listenerExceptions;
	private final String message;

	public EventDispatcherMultiException(Map<ActionListener, Exception> listenerExceptions) {
		super("");
		this.listenerExceptions = listenerExceptions;
		StringBuilder msg = new StringBuilder();
		if (listenerExceptions != null && listenerExceptions.size() > 0) {
			if (listenerExceptions.size() > 1) {
				msg.append("multiple listeners threw exceptions: ");
				msg.append(System.getProperty("line.separator"));				
			}
			for (ActionListener listener : listenerExceptions.keySet()) {
				msg.append(listener.getClass().getName());
				msg.append(": ");
				msg.append(listenerExceptions.get(listener).toString());
				msg.append(System.getProperty("line.separator"));
			}
		} else {
			msg.append("no exceptions");
		}
		message = msg.toString();
	}

	public Map<ActionListener, Exception> getListenerExceptions() {
		return listenerExceptions;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getMessage();
	}
}
