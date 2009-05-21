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
	public RemoveStoryFromStoryContainerEvent(Object source, Story story,
			StoryContainer storyContainer, Object destinationObject) {
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
