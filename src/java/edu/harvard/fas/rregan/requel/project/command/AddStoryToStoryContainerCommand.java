/*
 * $Id: AddStoryToStoryContainerCommand.java,v 1.2 2008/09/06 09:31:57 rregan Exp $
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
import edu.harvard.fas.rregan.requel.project.StoryContainer;

/**
 * @author ron
 */
public interface AddStoryToStoryContainerCommand extends EditCommand {

	/**
	 * Set the Story to add.
	 * 
	 * @param story
	 */
	public void setStory(Story story);

	/**
	 * @return the updated story.
	 */
	public Story getStory();

	/**
	 * Set the container this Story is being added to.
	 * 
	 * @param storyContainer
	 */
	public void setStoryContainer(StoryContainer storyContainer);

	/**
	 * @return the updated Story container.
	 */
	public StoryContainer getStoryContainer();
}
