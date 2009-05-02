/*
 * $Id: EditArgumentCommand.java,v 1.3 2009/02/17 11:50:48 rregan Exp $
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

import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Create or edit an argument for a position of an issue.
 * 
 * @author ron
 */
public interface EditArgumentCommand extends EditCommand {

	/**
	 * Set the argument to edit.
	 * 
	 * @param argument
	 */
	public void setArgument(Argument argument);

	/**
	 * @return the new or updated argument.
	 */
	public Argument getArgument();

	/**
	 * The position that the argument is for or against.
	 * 
	 * @param position
	 */
	public void setPosition(Position position);

	/**
	 * Set the text for the argument.
	 * 
	 * @param text
	 */
	public void setText(String text);

	/**
	 * Set the support level of the argument for or against the position.
	 * 
	 * @param supportLevelName
	 */
	public void setSupportLevelName(String supportLevelName);
}
