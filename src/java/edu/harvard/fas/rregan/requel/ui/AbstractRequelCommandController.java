/*
 * $Id: AbstractRequelCommandController.java,v 1.7 2008/12/13 00:42:02 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.ui;

import edu.harvard.fas.rregan.command.CommandFactory;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public abstract class AbstractRequelCommandController extends AbstractRequelController {

	private CommandFactory commandFactory;
	private CommandHandler commandHandler;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 */
	protected AbstractRequelCommandController(EventDispatcher eventDispatcher,
			CommandFactory commandFactory, CommandHandler commandHandler) {
		super(eventDispatcher);
		setCommandFactory(commandFactory);
		setCommandHandler(commandHandler);
	}

	/**
	 * @param commandFactory
	 * @param commandHandler
	 */
	protected AbstractRequelCommandController(CommandFactory commandFactory,
			CommandHandler commandHandler) {
		super();
		setCommandFactory(commandFactory);
		setCommandHandler(commandHandler);
	}

	protected <T> T getCommandFactory() {
		return (T) commandFactory;
	}

	protected void setCommandFactory(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}
}
