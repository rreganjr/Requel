/*
 * $Id: Command.java,v 1.1 2008/12/13 00:40:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

/**
 * A command represent an activity that changes the state of the application's
 * persistent state, such as adding or edting a user, project, or element of a
 * project. A command encompasses an atomic unit of work such that all the
 * changes made by the command succeed or fail in unison.<br>
 * A command my be a composite made up of multiple commands. In such a case the
 * changes made by all the consitutent commands either succeed or fail as a
 * single unit.<br>
 * Commands may be stateful and an instance of a command will only be used once
 * and discarded.
 * 
 * @author ron
 */
public interface Command {

	/**
	 * The execute() method entails the unit of work of the command. The
	 * execution will be transactional. If an exception is thrown from a command
	 * none of the work done by the command will be persisted. The state of the
	 * entity objects supplied to the command will be in the original state when
	 * supplied to the command.<br>
	 * If the command completes successfully the new state of changed entity
	 * objects will be returned through the public getter methods.<br>
	 * It is the resposability of the executor of the command to propogate the
	 * changes to entities made by a command to the rest of the system (such as
	 * any user interface objects.)
	 * 
	 * @throws Exception
	 */
	public void execute() throws Exception;

}
