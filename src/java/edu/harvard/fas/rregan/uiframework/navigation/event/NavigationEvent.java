/*
 * $Id: NavigationEvent.java,v 1.4 2008/09/12 01:00:58 rregan Exp $
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

import nextapp.echo2.app.event.ActionEvent;

/**
 * @author ron
 */
public class NavigationEvent extends ActionEvent {
	static final long serialVersionUID = 0;

	private final Object destinationObject;

	/**
	 * @param source
	 * @param command
	 */
	public NavigationEvent(Object source, String command) {
		this(source, command, null);
	}

	/**
	 * @param source
	 * @param command
	 * @param destinationObject -
	 *            the intended recipient of the event.
	 */
	public NavigationEvent(Object source, String command, Object destinationObject) {
		super(source, command);
		this.destinationObject = destinationObject;
	}

	/**
	 * The intended recipient of the event. this is for events such as an add
	 * entity event or remove entity event where a specific controller should
	 * recieve the event to do the adding or removing.
	 * 
	 * @return
	 */
	public Object getDestinationObject() {
		return destinationObject;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[source = " + getSource() + ", command = "
				+ getActionCommand() + ", destinationObject = " + destinationObject + "]";
	}
}
