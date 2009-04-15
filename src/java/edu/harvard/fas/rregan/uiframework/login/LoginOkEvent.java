/*
 * $Id: LoginOkEvent.java,v 1.3 2008/02/27 02:35:48 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.SetScreenEvent;

/**
 * This event is a response from the app specific login controller for
 * a LoginEvent that succeeded. It contains the user and next screen to
 * display.
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
