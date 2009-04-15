/*
 * $Id: NavigationEvent.java,v 1.4 2008/09/12 01:00:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
	 * @param destinationObject - the intended recipient of the event.
	 */
	public NavigationEvent(Object source, String command, Object destinationObject) {
		super(source, command);
		this.destinationObject = destinationObject;
	}

	/**
	 * The intended recipient of the event. this is for events such as
	 * an add entity event or remove entity event where a specific controller
	 * should recieve the event to do the adding or removing.
	 * 
	 * @return
	 */
	public Object getDestinationObject() {
		return destinationObject;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[source = " + getSource() + ", command = " + getActionCommand() + ", destinationObject = " + destinationObject  + "]";
	}
}
