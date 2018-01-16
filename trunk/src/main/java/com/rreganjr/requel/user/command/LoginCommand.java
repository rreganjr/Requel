/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.user.command;

import com.rreganjr.command.Command;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * @author ron
 */
public interface LoginCommand extends Command {

	/**
	 * @param username -
	 *            the username of the user account to login
	 */
	public void setUsername(String username);

	/**
	 * @param password -
	 *            the password of the user account to login
	 */
	public void setPassword(String password);

	/**
	 * @return the logged in user
	 * @throws NoSuchUserException -
	 *             if the username does not refer to a known user or the
	 *             password doesn't match the users password.
	 */
	public User getUser() throws NoSuchUserException;
}
