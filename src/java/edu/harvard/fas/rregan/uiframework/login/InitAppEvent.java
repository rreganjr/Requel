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

import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event gets fired after a successful login by the LoginOkController.
 * Controllers that initialize the UI and state of the application should listen
 * for this event and configure the app.
 * 
 * @author ron
 */
public class InitAppEvent extends NavigationEvent {
	static final long serialVersionUID = 0L;

	private Object user;

	/**
	 * @param source
	 * @param user
	 */
	public InitAppEvent(Object source, Object user) {
		super(source, InitAppEvent.class.getName());
		setUser(user);
	}

	/**
	 * If the app has user specific initialization, controllers that do the
	 * initialization can use the user attached to this event.
	 * 
	 * @return
	 */
	public Object getUser() {
		return user;
	}

	protected void setUser(Object user) {
		this.user = user;
	}
}
