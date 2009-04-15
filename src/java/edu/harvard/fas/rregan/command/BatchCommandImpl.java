/*
 * $Id: BatchCommandImpl.java,v 1.1 2008/12/13 00:41:04 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

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
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		for (int i = 0; i < commands.size(); i++) {
			commands.set(i, commandHandler.execute(commands.get(i)));
		}
	}
}
