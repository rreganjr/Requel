/*
 * $Id$
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
		if ((listenerExceptions != null) && (listenerExceptions.size() > 0)) {
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
