/*
 * $Id: LoginFailedEvent.java,v 1.2 2008/02/27 02:35:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event is a response from the app specific login controller for
 * a LoginEvent when the supplied user credentials are not valid or the
 * controller failed for whatever reason. It contains a message on why
 * the login didn't succeed.
 * 
 * @author ron
 */
public class LoginFailedEvent extends NavigationEvent {
	static final long serialVersionUID = 0L;

	private String message;
	
	public LoginFailedEvent(Object source, String message) {
		super(source, LoginFailedEvent.class.getName());
		setMessage(message);
	}

	public String getMessage() {
		return message;
	}

	protected void setMessage(String message) {
		this.message = message;
	}
	
	
}
