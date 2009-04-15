/*
 * $Id: RemoveGoalFromGoalContainerEvent.java,v 1.1 2008/09/12 22:44:21 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
	public RemoveGoalFromGoalContainerEvent(Object source, Goal goal, GoalContainer goalContainer, Object destinationObject) {
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
