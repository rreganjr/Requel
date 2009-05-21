/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.user.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.command.LoginCommand;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * @author ron
 */
@Component("loginCommand")
@Scope("prototype")
public class LoginCommandImpl extends AbstractUserCommand implements LoginCommand {

	private String username;
	private String password;
	private User user;

	/**
	 * @param userRepository
	 */
	@Autowired
	public LoginCommandImpl(UserRepository userRepository) {
		super(userRepository);
	}

	public void execute() {
		User user = getUserRepository().findUserByUsername(getUsername());
		if (user.isPassword(password)) {
			setUser(user);
		} else {
			throw NoSuchUserException.wrongPasswordForUser(user);
		}
	}

	public User getUser() throws NoSuchUserException {
		return user;
	}

	protected void setUser(User user) {
		this.user = user;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	protected String getUsername() {
		return username;
	}

	protected String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}
}
