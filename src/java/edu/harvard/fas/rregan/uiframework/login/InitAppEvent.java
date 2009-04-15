/*
 * $Id: InitAppEvent.java,v 1.2 2008/09/12 00:15:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
