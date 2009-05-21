/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Story;

/**
 * Given a story, create a new story copying references to actors and goals. If
 * a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyStoryCommand extends EditCommand {

	/**
	 * @param Story -
	 *            the Story to copy.
	 */
	public void setOriginalStory(Story story);

	/**
	 * @param newName -
	 *            the name for the new story. if this is not set, or is set to a
	 *            name already in use, a unique name will be generated.
	 */
	public void setNewStoryName(String newName);

	/**
	 * @return the new copy of the use case.
	 */
	public Story getNewStory();
}
