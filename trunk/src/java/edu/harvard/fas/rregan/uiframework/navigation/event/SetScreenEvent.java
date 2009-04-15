/*
 * $Id: SetScreenEvent.java,v 1.3 2008/03/01 02:30:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.navigation.event;

import org.apache.log4j.Logger;

/**
 * A base class for events that cause the UIFrameworkApp's screen to change.
 * @author ron
 */
public class SetScreenEvent extends NavigationEvent {
	private static final Logger log = Logger.getLogger(SetScreenEvent.class);
	static final long serialVersionUID = 0;

	private String screenToDisplay;

	/**
	 * @param source
	 * @param command
	 * @param screenToDisplay
	 */
	protected SetScreenEvent(Object source, String command, String screenToDisplay) {
		super(source, command);
		setScreenToDisplay(screenToDisplay);
	}

	/**
	 * @return - the new Screen to display
	 */
	public String getScreenToDisplay() {
		return screenToDisplay;
	}

	protected void setScreenToDisplay(String screenToDisplay) {
		this.screenToDisplay = screenToDisplay;
	}
}
