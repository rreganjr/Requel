/*
 * $Id: DeleteGoalCommand.java,v 1.1 2008/11/05 18:25:52 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Goal;

/**
 * 
 * @author ron
 */
public interface DeleteGoalCommand extends EditCommand {

	/**
	 * Set the goal to delete.
	 * @param goal
	 */
	public void setGoal(Goal goal);
}
