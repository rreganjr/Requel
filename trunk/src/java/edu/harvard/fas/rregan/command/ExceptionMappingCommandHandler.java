/*
 * $Id: ExceptionMappingCommandHandler.java,v 1.2 2008/12/18 12:05:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
