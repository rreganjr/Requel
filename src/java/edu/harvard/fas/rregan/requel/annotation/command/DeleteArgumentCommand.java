/*
 * $Id: DeleteArgumentCommand.java,v 1.1 2009/02/13 12:08:01 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Delete an argument of a position from the system.
 * 
 * @author ron
 */
public interface DeleteArgumentCommand extends EditCommand {

	/**
	 * Set the argument to delete.
	 * 
	 * @param argument
	 */
	public void setArgument(Argument argument);
}
