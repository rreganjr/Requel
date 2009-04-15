/*
 * $Id: CommandFactory.java,v 1.1 2008/12/13 00:41:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

/**
 * A CommandFactory is used to get fresh instances of a command. Each command
 * instance should only be used once and discarded.<br>
 * A CommandFactory has a strategy for creating new commands by command type.
 * 
 * @see CommandFactoryStrategy
 * @author ron
 */
public interface CommandFactory {

	public BatchCommand newBatchCommand();

}