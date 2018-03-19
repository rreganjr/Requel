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