/*
 * $Id: GoalContainer.java,v 1.4 2008/09/01 08:40:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Comparator;
import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

/**
 * A thing that can contain/refer to goals.
 * 
 * @author ron
 */
public interface GoalContainer extends Describable, CreatedEntity {
	/**
	 * The goals referenced.
	 * 
	 * @return
	 */
	public Set<Goal> getGoals();

	/**
	 * Compare the objects that contain goals by the description.
	 */
	public static final Comparator<GoalContainer> COMPARATOR = new GoalContainerComparator();

	/**
	 * A Comparator for collections of goal containers.
	 */
	public static class GoalContainerComparator implements Comparator<GoalContainer> {
		@Override
		public int compare(GoalContainer o1, GoalContainer o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
