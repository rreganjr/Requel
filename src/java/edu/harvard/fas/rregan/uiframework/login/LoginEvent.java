/*
 * $Id: LoginEvent.java,v 1.2 2008/02/27 02:35:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event indicates a user requesting to log into the system with
 * the supplied username and password. This event typically flows from
 * a UI component (the login screen) to an app specific controller for
 * processing login requests.
 * 
 * @author ron
 */
public class LoginEvent extends NavigationEvent {
	static final long serialVersionUID = 0L;

	private String username;
	private String password;

	/**
	 * @param source
	 * @param username
	 * @param password
	 */
	public LoginEvent(Object source, String username, String password) {
		super(source, LoginEvent.class.getName());
		setUsername(username);
		setPassword(password);
	}

	/**
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	protected void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	protected void setPassword(String password) {
		this.password = password;
	}

}
