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
 * This event is a response from the app specific login controller for a
 * LoginEvent that succeeded. It contains the user and next screen to display.
 * 
 * @author ron
 */
public class LoginOkEvent extends SetScreenEvent {
	static final long serialVersionUID = 0L;

	private Object user;

	/**
	 * @param source
	 * @param user
	 * @param screenNameToDisplay
	 */
	public LoginOkEvent(Object source, Object user, String screenNameToDisplay) {
		super(source, LoginOkEvent.class.getName(), screenNameToDisplay);
		setUser(user);
	}

	/**
	 * @return - the user that logged in as an Object
	 */
	public Object getUser() {
		return user;
	}

	protected void setUser(Object user) {
		this.user = user;
	}

}
