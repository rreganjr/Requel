/*
 * $Id: ExceptionMappingCommandHandler.java,v 1.2 2008/12/18 12:05:38 rregan Exp $
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
package edu.harvard.fas.rregan.command;

import edu.harvard.fas.rregan.repository.jpa.ExceptionMapper;

/**
 * A command handler that wraps another command handler catching exceptions
 * thrown and converting them to application specific exceptions by a
 * customizable exception mapper.
 * 
 * @author ron
 */
public class ExceptionMappingCommandHandler implements CommandHandler {

	private final ExceptionMapper exceptionMapper;
	private final CommandHandler commandHandler;

	/**
	 * @param exceptionMapper
	 * @param commandHandler
	 */
	public ExceptionMappingCommandHandler(ExceptionMapper exceptionMapper,
			CommandHandler commandHandler) {
		this.exceptionMapper = exceptionMapper;
		this.commandHandler = commandHandler;
	}

	public <T extends Command> T execute(T command) throws Exception {
		try {
			return commandHandler.execute(command);
		} catch (Exception e) {
			throw exceptionMapper.convertException(e);
		}
	}
}
