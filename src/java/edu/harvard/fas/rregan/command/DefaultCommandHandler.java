/*
 * $Id: DefaultCommandHandler.java,v 1.1 2009/04/01 15:45:47 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.requel.user.exception.NoSuchOrganizationException;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * The default handler executes the command in a transaction locally to the
 * process that supplied the command.
 * 
 * @author ron
 */
public class DefaultCommandHandler implements CommandHandler {
	@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = {
			NoSuchOrganizationException.class, NoSuchUserException.class })
	public <T extends Command> T execute(T command) throws Exception {
		command.execute();
		return command;
	}
}
