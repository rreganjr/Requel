/*
 * $Id: DeletePositionCommand.java,v 1.1 2009/02/13 12:08:02 rregan Exp $
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
