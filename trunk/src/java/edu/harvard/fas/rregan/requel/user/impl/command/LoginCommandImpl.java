/*
 * $Id: LoginCommandImpl.java,v 1.4 2008/08/25 02:20:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
