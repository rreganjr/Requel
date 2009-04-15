/*
 * $Id: RemoveStoryFromStoryContainerCommand.java,v 1.1 2008/09/03 02:56:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;

/**
 * @author ron
 */
public interface RemoveStoryFromStoryContainerCommand extends EditCommand {

	/**
	 * Set the Story to remove.
	 * 
	 * @param story
	 */
	public void setStory(Story story);

	/**
	 * @return the updated Story.
	 */
	public Story getStory();

	/**
	 * Set the container this Story is being removed from.
	 * 
	 * @param storyContainer
	 */
	public void setStoryContainer(StoryContainer storyContainer);

	/**
	 * @return the updated Story container.
	 */
	public StoryContainer getStoryContainer();
}
