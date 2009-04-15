/*
 * $Id: AddStoryToStoryContainerCommand.java,v 1.2 2008/09/06 09:31:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
