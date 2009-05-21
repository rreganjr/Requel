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
 * The logout event gets fired when a user indicates they want to logout of the
 * application. The event should be fired by a NavigatorButton visible to the
 * user after they login to the system.<br>
 * <code>
 * NavigatorButton logoutButton = 
 * 		new NavigatorButton("Logout", getEventDispatcher(), new LogoutEvent(mainScreen));
 * </code>
 * <br>
 * The app should have a LogoutController that does any necissary cleanup and
 * fires a LogoutOkEvent for the LogoutOkController to update the UI.
 * 
 * @author ron
 */
public class LogoutEvent extends NavigationEvent {
	static final long serialVersionUID = 0L;

	public static final String CMD_LOGOUT = LogoutEvent.class.getName();

	/**
	 * @param source
	 * @param command
	 */
	public LogoutEvent(Object source) {
		super(source, CMD_LOGOUT);
	}

}
