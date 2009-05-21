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
