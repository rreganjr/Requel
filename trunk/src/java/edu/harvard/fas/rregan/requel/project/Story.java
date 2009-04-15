/*
 * $Id: Story.java,v 1.5 2008/09/06 09:31:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

/**
 * @author ron
 */
public interface Story extends TextEntity, GoalContainer, ActorContainer, Comparable<Story> {

	/**
	 * Return the project entities that have a direct reference to this story.
	 * 
	 * @return
	 */
	public Set<StoryContainer> getReferers();

	/**
	 * @return The type of story: Success or Exceptional
	 */
	public StoryType getStoryType();

	/**
	 * Set the type of story.
	 * 
	 * @param storyType
	 */
	public void setStoryType(StoryType storyType);

}
