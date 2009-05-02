/*
 * $Id: EditStoryCommand.java,v 1.4 2009/01/27 09:30:17 rregan Exp $
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

import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;

/**
 * @author ron
 */
public interface EditStoryCommand extends EditTextEntityCommand {

	/**
	 * Set the Story to edit.
	 * 
	 * @param Story
	 */
	public void setStory(Story Story);

	/**
	 * Get the new or updated Story.
	 * 
	 * @return
	 */
	public Story getStory();

	/**
	 * Set the container this Story is being added to.
	 * 
	 * @param StoryContainer
	 */
	public void setStoryContainer(StoryContainer StoryContainer);

	/**
	 * @param storyTypeName -
	 *            the name of the type of story
	 */
	public void setStoryTypeName(String storyTypeName);

}
