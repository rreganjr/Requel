/*
 * $Id: RemoveStoryFromStoryContainerEvent.java,v 1.1 2008/09/12 22:44:20 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.ui.project;

import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event is fired from the GoalsTable when a user clicks the "remove"
 * button to remove the goal from the goal container.
 * 
 * @author ron
 */
public class RemoveStoryFromStoryContainerEvent extends NavigationEvent {
	static final long serialVersionUID = 0;

	private final Story story;
	private final StoryContainer storyContainer;

	/**
	 * @param source
	 * @param goal
	 * @param goalContainer
	 * @param destinationObject
	 */
	public RemoveStoryFromStoryContainerEvent(Object source, Story story, StoryContainer storyContainer, Object destinationObject) {
		super(source, RemoveStoryFromStoryContainerEvent.class.getName(), destinationObject);
		this.story = story;
		this.storyContainer = storyContainer;
	}

	/**
	 * @return the goal to remove from the container.
	 */
	public Story getStory() {
		return story;
	}

	/**
	 * @return the container to remove the goal from.
	 */
	public StoryContainer getStoryContainer() {
		return storyContainer;
	}
}
