/*
 * $Id: LoginCommand.java,v 1.3 2008/12/13 00:40:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
