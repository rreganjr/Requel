/*
 * $Id: LogoutOkEvent.java,v 1.3 2008/02/27 02:35:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.SetScreenEvent;

/**
 * The LogoutOkEvent should be fired by the app specific LogoutController
 * after recieving a LogoutEvent if the state of the app is ok for logging
 * out the user. The default constructor is hardwired to set the LoginScreen
 * as the current screen.<br>
 * The LogoutOkEvent is handled by the UIFramework LogoutOkController.
 * @author ron
 */
public class LogoutOkEvent extends SetScreenEvent {
	static final long serialVersionUID = 0L;

	/**
	 * Create a LogoutOkEvent that resets the screen to the UIFramework
	 * standard LoginScreen.
	 * @param source
	 */
	public LogoutOkEvent(Object source) {
		this(source, LoginScreen.screenName);
	}

	/**
	 * Create a LogoutOkEvent that resets the screen to the supplied
	 * screen.
	 * 
	 * @param source
	 * @param screenName
	 */
	public LogoutOkEvent(Object source, String screenName) {
		super(source, LogoutOkEvent.class.getName(), screenName);
	}
}
