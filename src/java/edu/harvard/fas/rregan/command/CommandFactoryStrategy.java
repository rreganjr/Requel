/*
 * $Id: CommandFactoryStrategy.java,v 1.1 2008/12/13 00:40:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

/**
 * Each CommandFactory uses a strategy to create new command instances. A
 * strategy may get commands from a container (such as the Spring IOC container
 * or a J2EE container where the commands are stateful session beans) or create
 * commands directly.
 * 
 * @author ron
 */
public interface CommandFactoryStrategy {

	/**
	 * Create a new instance of a command or return a "clean" instance of a
	 * command where the state would be the same as if a new command were
	 * returned.<br>
	 * The returned command must be guarenteed to be uniquely referred to by a
	 * single client at a time. A command will not be thread-safe.
	 * 
	 * @param commandType -
	 *            the type of command to create
	 * @return a "clean" instance of a command
	 */
	public Command newInstance(Class<? extends Command> commandType);
}
