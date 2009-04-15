/*
 * $Id: AbstractCommand.java,v 1.1 2008/12/13 00:41:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.repository.Repository;

/**
 * @author ron
 */
public abstract class AbstractCommand implements Command {
	protected static final Logger log = Logger.getLogger(AbstractCommand.class);

	private final Repository repository;

	protected AbstractCommand(Repository repository) {
		this.repository = repository;
	}
	
	protected Repository getRepository() {
		return repository;
	}
}
