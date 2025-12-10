/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;

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
