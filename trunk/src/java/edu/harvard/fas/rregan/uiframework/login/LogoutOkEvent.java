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
package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.SetScreenEvent;

/**
 * The LogoutOkEvent should be fired by the app specific LogoutController after
 * recieving a LogoutEvent if the state of the app is ok for logging out the
 * user. The default constructor is hardwired to set the LoginScreen as the
 * current screen.<br>
 * The LogoutOkEvent is handled by the UIFramework LogoutOkController.
 * 
 * @author ron
 */
public class LogoutOkEvent extends SetScreenEvent {
	static final long serialVersionUID = 0L;

	/**
	 * Create a LogoutOkEvent that resets the screen to the UIFramework standard
	 * LoginScreen.
	 * 
	 * @param source
	 */
	public LogoutOkEvent(Object source) {
		this(source, LoginScreen.screenName);
	}

	/**
	 * Create a LogoutOkEvent that resets the screen to the supplied screen.
	 * 
	 * @param source
	 * @param screenName
	 */
	public LogoutOkEvent(Object source, String screenName) {
		super(source, LogoutOkEvent.class.getName(), screenName);
	}
}
