/*
 * $Id: LoginEvent.java,v 1.2 2008/02/27 02:35:47 rregan Exp $
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
 * This event indicates a user requesting to log into the system with the
 * supplied username and password. This event typically flows from a UI
 * component (the login screen) to an app specific controller for processing
 * login requests.
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
