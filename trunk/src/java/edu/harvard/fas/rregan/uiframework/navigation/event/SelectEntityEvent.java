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
 * An event fired by a selection panel to indicate the object selected and the
 * desired destination of the selected object if required.
 * 
 * @author ron
 */
public class SelectEntityEvent extends ClosePanelEvent {
	static final long serialVersionUID = 0;

	private final Object selectedObject;

	/**
	 * @param source -
	 *            the panel firing the event, which should also be closed after
	 *            firing.
	 * @param selectedObject -
	 *            the object that was selected.
	 * @param destinationObject -
	 *            the orginal object that requested the selection of an object,
	 *            this will be used to dispatch the event to the appropriate
	 *            requestor in case there are other listeners for selection of
	 *            other objects. If this is null all listeners for select events
	 *            will recieve the event.
	 */
	public SelectEntityEvent(Panel source, Object selectedObject, Object destinationObject) {
		this(source, source, selectedObject, destinationObject);
	}

	/**
	 * Create a select event when the source is not the panel to be closed,
	 * possibly a controller or button on the panel.
	 * 
	 * @param source -
	 *            the originator of the event
	 * @param panelToClose -
	 *            the panel to close after the event is fired.
	 * @param selectedObject
	 * @param destinationObject
	 */
	public SelectEntityEvent(Object source, Panel panelToClose, Object selectedObject,
			Object destinationObject) {
		this(source, SelectEntityEvent.class.getName(), panelToClose, selectedObject,
				destinationObject);
	}

	protected SelectEntityEvent(Object source, String command, Panel panelToClose,
			Object selectedObject, Object destinationObject) {
		super(source, command, panelToClose, destinationObject);
		this.selectedObject = selectedObject;
	}

	public Object getObject() {
		return selectedObject;
	}
}
