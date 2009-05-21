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
