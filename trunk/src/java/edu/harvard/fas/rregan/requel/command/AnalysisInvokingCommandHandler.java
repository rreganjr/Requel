/*
 * $Id: AnalysisInvokingCommandHandler.java,v 1.1 2009/02/13 12:08:04 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.command.CommandHandler;

/**
 * A CommandHandler decorator that executes a command, and then if it implements
 * the EditAnalyzableCommand interface and didn't through an exception it calls
 * the invokeAnalysis() method on the command.
 * 
 * @author ron
 */
public class AnalysisInvokingCommandHandler implements CommandHandler {

	private final CommandHandler commandHandler;

	/**
	 * @param exceptionMapper
	 * @param commandHandler
	 */
	public AnalysisInvokingCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	public <T extends Command> T execute(T command) throws Exception {
		T executedCommand = commandHandler.execute(command);
		if (executedCommand instanceof AnalyzableEditCommand) {
			((AnalyzableEditCommand) executedCommand).invokeAnalysis();
		}
		return executedCommand;
	}
}
