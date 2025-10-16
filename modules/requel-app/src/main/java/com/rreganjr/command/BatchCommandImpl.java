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
package com.rreganjr.command;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Execute a series of commands. If execution fails the results in getCommands()
 * may be inconsistant.
 * 
 * @author ron
 */
@Controller("batchCommand")
@Scope("prototype")
public class BatchCommandImpl implements BatchCommand {

	private final CommandHandler commandHandler;
	private final List<Command> commands = new LinkedList<Command>();

	/**
	 * @param commandHandler
	 */
	@Autowired
	public BatchCommandImpl(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	public void addCommand(Command command) {
		commands.add(command);
	}

	public void addCommands(Collection<Command> commands) {
		commands.addAll(commands);
	}

	public Collection<Command> getCommands() {
		return commands;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		for (int i = 0; i < commands.size(); i++) {
			commands.set(i, commandHandler.execute(commands.get(i)));
		}
	}
}
