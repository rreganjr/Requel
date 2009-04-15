/*
 * $Id: StoryContainer.java,v 1.1 2008/09/03 02:56:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Comparator;
import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

/**
 * A thing that can contain/refer to stories.
 * 
 * @author ron
 */
public interface StoryContainer extends Describable, CreatedEntity {
	/**
	 * The stories referenced.
	 * 
	 * @return
	 */
	public Set<Story> getStories();

	/**
	 * Compare the objects that contain Storys by the description.
	 */
	public static final Comparator<StoryContainer> COMPARATOR = new StoryContainerComparator();

	/**
	 * A Comparator for collections of Story containers.
	 */
	public static class StoryContainerComparator implements Comparator<StoryContainer> {
		@Override
		public int compare(StoryContainer o1, StoryContainer o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
