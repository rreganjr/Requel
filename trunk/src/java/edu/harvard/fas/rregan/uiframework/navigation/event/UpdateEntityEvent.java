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

import edu.harvard.fas.rregan.uiframework.panel.Panel;

/**
 * An event notification indicating that a data object has changed and
 * optionally close a panel.
 * 
 * @author ron
 */
public class UpdateEntityEvent extends ClosePanelEvent {
	static final long serialVersionUID = 0;

	private final Object updatedObject;

	/**
	 * Create an event notification from the specified panel, indicating to
	 * close that panel.
	 * 
	 * @param source
	 * @param updatedObject
	 */
	public UpdateEntityEvent(Panel source, Object updatedObject) {
		this(source, source, updatedObject);
	}

	public UpdateEntityEvent(Object source, Panel panelToClose, Object updatedObject) {
		this(source, UpdateEntityEvent.class.getName(), panelToClose, updatedObject);
	}

	// note the destinationObject parameter to the ClasePanelEvent super
	// constructor should
	// always be null so that all update listeners for an object get the event.
	protected UpdateEntityEvent(Object source, String command, Panel panelToClose,
			Object updatedObject) {
		super(source, command, panelToClose, null);
		this.updatedObject = updatedObject;
	}

	public Object getObject() {
		return updatedObject;
	}
}
