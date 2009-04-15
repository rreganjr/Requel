/*
 * $Id: SelectEntityEvent.java,v 1.3 2008/09/12 00:15:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
	 * @param source - the panel firing the event, which should also be closed after firing.
	 * @param selectedObject - the object that was selected.
	 * @param destinationObject - the orginal object that requested the selection of an object,
	 *        this will be used to dispatch the event to the appropriate requestor in case there
	 *        are other listeners for selection of other objects. If this is null all listeners
	 *        for select events will recieve the event.
	 */
	public SelectEntityEvent(Panel source, Object selectedObject, Object destinationObject) {
		this(source, source, selectedObject, destinationObject);
	}

	/**
	 * Create a select event when the source is not the panel to be closed, possibly a controller or button on the panel.
	 * @param source - the originator of the event
	 * @param panelToClose - the panel to close after the event is fired.
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
