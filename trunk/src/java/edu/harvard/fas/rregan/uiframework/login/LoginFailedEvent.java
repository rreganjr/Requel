/*
 * $Id: LoginFailedEvent.java,v 1.2 2008/02/27 02:35:46 rregan Exp $
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
 * This event is a response from the app specific login controller for a
 * LoginEvent when the supplied user credentials are not valid or the controller
 * failed for whatever reason. It contains a message on why the login didn't
 * succeed.
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
