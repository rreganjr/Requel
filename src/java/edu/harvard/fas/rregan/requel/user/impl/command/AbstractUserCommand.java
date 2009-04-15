/*
 * $Id: AbstractUserCommand.java,v 1.4 2008/12/13 00:41:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user.impl.command;

import edu.harvard.fas.rregan.command.AbstractCommand;
import edu.harvard.fas.rregan.requel.user.UserRepository;

/**
 * @author ron
 */
public abstract class AbstractUserCommand extends AbstractCommand {
	protected AbstractUserCommand(UserRepository userRepository) {
		super(userRepository);
	}

	protected UserRepository getUserRepository() {
		return (UserRepository) getRepository();
	}
}
