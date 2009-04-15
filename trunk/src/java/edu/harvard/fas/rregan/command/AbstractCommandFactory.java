/*
 * $Id: AbstractCommandFactory.java,v 1.1 2008/12/13 00:40:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

/**
 * A base command factory that supplied the creation strategy to implementation
 * specific command factories.
 * 
 * @author ron
 */
public class AbstractCommandFactory implements CommandFactory {
	private final CommandFactoryStrategy creationStrategy;

	protected AbstractCommandFactory(CommandFactoryStrategy creationStrategy) {
		this.creationStrategy = creationStrategy;
	}

	protected CommandFactoryStrategy getCreationStrategy() {
		return creationStrategy;
	}

	@Override
	public BatchCommand newBatchCommand() {
		return (BatchCommand) getCreationStrategy().newInstance(BatchCommandImpl.class);
	}

}
