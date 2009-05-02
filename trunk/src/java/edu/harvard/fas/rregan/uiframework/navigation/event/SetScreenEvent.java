/*
 * $Id: SetScreenEvent.java,v 1.3 2008/03/01 02:30:03 rregan Exp $
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

import org.apache.log4j.Logger;

/**
 * A base class for events that cause the UIFrameworkApp's screen to change.
 * 
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
