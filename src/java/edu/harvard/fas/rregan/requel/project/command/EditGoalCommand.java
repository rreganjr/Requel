/*
 * $Id: EditGoalCommand.java,v 1.5 2009/01/27 09:30:17 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;

/**
 * @author ron
 */
public interface EditGoalCommand extends EditTextEntityCommand {

	/**
	 * Set the goal to edit.
	 * 
	 * @param goal
	 */
	public void setGoal(Goal goal);

	/**
	 * Get the new or updated goal.
	 * 
	 * @return
	 */
	public Goal getGoal();

	/**
	 * Set the container this goal is being added to.
	 * 
	 * @param goalContainer
	 */
	public void setGoalContainer(GoalContainer goalContainer);
}
