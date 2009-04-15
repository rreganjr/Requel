/*
 * $Id: EditStoryCommand.java,v 1.4 2009/01/27 09:30:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
