/*
 * $Id: UserCommandFactory.java,v 1.3 2008/12/13 00:40:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.user.command;

import edu.harvard.fas.rregan.command.CommandFactory;

/**
 * @author ron
 */
public interface UserCommandFactory extends CommandFactory {

	/**
	 * @return a new LoginCommand for authenticating a user of the system.
	 */
	public LoginCommand newLoginCommand();

	/**
	 * @return a new EditUserCommand for creating or editing a user of the
	 *         system.
	 */
	public EditUserCommand newEditUserCommand();

}
