/*
 * $Id: CommandHandler.java,v 1.1 2008/12/13 00:40:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

/**
 * The context for the execution of a command. A handler demarcates the
 * transactionality of a command. A handler may execute the command locally or
 * remotely transparently from the process that supplies the command for
 * execution.
 * 
 * @author ron
 */
public interface CommandHandler {

	/**
	 * @param <T>
	 *            The type of command
	 * @param command -
	 *            the command to execute and return on success.
	 * @return a command that executed successfully.
	 * @throws Exception
	 */
	public <T extends Command> T execute(T command) throws Exception;

}
