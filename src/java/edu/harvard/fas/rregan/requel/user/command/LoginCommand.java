/*
 * $Id: LoginCommand.java,v 1.3 2008/12/13 00:40:44 rregan Exp $
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
package edu.harvard.fas.rregan.requel.user.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

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