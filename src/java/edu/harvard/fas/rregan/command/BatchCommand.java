/*
 * $Id: BatchCommand.java,v 1.1 2008/12/13 00:41:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

import java.util.Collection;

/**
 * Execute a series of commands. If execution fails the results in getCommands()
 * may be inconsistant.
 * 
 * @author ron
 */
public interface BatchCommand extends Command {

	public void addCommand(Command command);

	public void addCommands(Collection<Command> commands);

	public Collection<Command> getCommands();
}
