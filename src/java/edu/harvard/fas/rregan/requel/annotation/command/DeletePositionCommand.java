/*
 * $Id: DeletePositionCommand.java,v 1.1 2009/02/13 12:08:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation.command;

import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Delete a position from an issue and all its arguments.
 * 
 * @author ron
 */
public interface DeletePositionCommand extends EditCommand {

	/**
	 * Set the position to delete.
	 * 
	 * @param position
	 */
	public void setPosition(Position position);
}
