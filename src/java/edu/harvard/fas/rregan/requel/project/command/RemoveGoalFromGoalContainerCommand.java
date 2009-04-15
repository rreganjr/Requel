/*
 * $Id: RemoveGoalFromGoalContainerCommand.java,v 1.1 2008/05/01 08:10:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;

/**
 * @author ron
 */
public interface RemoveGoalFromGoalContainerCommand extends EditCommand {

	/**
	 * Set the goal to remove.
	 * 
	 * @param goal
	 */
	public void setGoal(Goal goal);

	/**
	 * @return the updated goal.
	 */
	public Goal getGoal();

	/**
	 * Set the container this goal is being removed from.
	 * 
	 * @param goalContainer
	 */
	public void setGoalContainer(GoalContainer goalContainer);

	/**
	 * @return the updated goal container.
	 */
	public GoalContainer getGoalContainer();
}
