/*
 * $Id: RemoveGoalFromGoalContainerEvent.java,v 1.1 2008/09/12 22:44:21 rregan Exp $
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

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;

/**
 * This event is fired from the GoalsTable when a user clicks the "remove"
 * button to remove the goal from the goal container.
 * 
 * @author ron
 */
public class RemoveGoalFromGoalContainerEvent extends NavigationEvent {
	static final long serialVersionUID = 0;

	private final Goal goal;
	private final GoalContainer goalContainer;

	/**
	 * @param source
	 * @param goal
	 * @param goalContainer
	 * @param destinationObject
	 */
	public RemoveGoalFromGoalContainerEvent(Object source, Goal goal, GoalContainer goalContainer,
			Object destinationObject) {
		super(source, RemoveGoalFromGoalContainerEvent.class.getName(), destinationObject);
		this.goal = goal;
		this.goalContainer = goalContainer;
	}

	/**
	 * @return the goal to remove from the container.
	 */
	public Goal getGoal() {
		return goal;
	}

	/**
	 * @return the container to remove the goal from.
	 */
	public GoalContainer getGoalContainer() {
		return goalContainer;
	}
}
