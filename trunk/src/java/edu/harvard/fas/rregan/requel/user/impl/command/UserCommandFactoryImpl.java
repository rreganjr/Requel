/*
 * $Id: UserCommandFactoryImpl.java,v 1.6 2008/12/13 00:41:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.AbstractCommandFactory;
import edu.harvard.fas.rregan.command.CommandFactoryStrategy;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.command.LoginCommand;
import edu.harvard.fas.rregan.requel.user.command.UserCommandFactory;

/**
 * @author ron
 */
@Controller("userCommandFactory")
@Scope("singleton")
public class UserCommandFactoryImpl extends AbstractCommandFactory implements UserCommandFactory {

	/**
	 * @param creationStrategy -
	 *            the strategy to use for creating new Command instances
	 */
	@Autowired
	public UserCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public LoginCommand newLoginCommand() {
		return (LoginCommand) getCreationStrategy().newInstance(LoginCommandImpl.class);
	}

	@Override
	public EditUserCommand newEditUserCommand() {
		return (EditUserCommand) getCreationStrategy().newInstance(EditUserCommandImpl.class);
	}

}
