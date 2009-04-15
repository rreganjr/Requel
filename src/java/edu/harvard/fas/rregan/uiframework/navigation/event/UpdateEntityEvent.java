/*
 * $Id: UpdateEntityEvent.java,v 1.7 2008/09/12 01:00:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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

	// note the destinationObject parameter to the ClasePanelEvent super constructor should
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
