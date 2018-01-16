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
