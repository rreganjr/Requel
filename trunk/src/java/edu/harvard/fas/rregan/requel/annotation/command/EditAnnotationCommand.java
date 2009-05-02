/*
 * $Id: EditAnnotationCommand.java,v 1.1 2008/09/04 09:47:19 rregan Exp $
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

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.command.EditCommand;

/**
 * Base class for annotation editing commands.
 * 
 * @author ron
 */
public interface EditAnnotationCommand extends EditCommand {

	/**
	 * Set the object used for grouping the annotation.
	 * 
	 * @param groupingObject -
	 *            a persistent object
	 */
	public void setGroupingObject(Object groupingObject);

	/**
	 * Set the text for the issue.
	 * 
	 * @param text
	 */
	public void setText(String text);

	/**
	 * Set the thing being annotated with the issue.
	 * 
	 * @param annotatable -
	 *            set the entity being annotated.
	 */
	public void setAnnotatable(Annotatable annotatable);
}
