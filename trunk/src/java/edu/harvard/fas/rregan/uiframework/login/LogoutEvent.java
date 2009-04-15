/*
 * $Id: LogoutEvent.java,v 1.2 2008/02/27 02:35:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * The logout event gets fired when a user indicates they want to logout
 * of the application. The event should be fired by a NavigatorButton
 * visible to the user after they login to the system.<br>
 * <code>
 * NavigatorButton logoutButton = 
 * 		new NavigatorButton("Logout", getEventDispatcher(), new LogoutEvent(mainScreen));
 * </code>
 * <br>
 * The app should have a LogoutController that does any necissary cleanup
 * and fires a LogoutOkEvent for the LogoutOkController to update the UI.
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
