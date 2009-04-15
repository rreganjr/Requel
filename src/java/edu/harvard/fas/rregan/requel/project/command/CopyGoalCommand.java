/*
 * $Id: CopyGoalCommand.java,v 1.1 2008/09/26 01:35:04 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.Goal;

/**
 * Given a goal, create a new goal copying references to actors and goals.
 * If a name isn't supplied a unique name is generated.
 * 
 * @author ron
 */
public interface CopyGoalCommand extends EditCommand {

	/**
	 * @param Goal -
	 *            the Goal to copy.
	 */
	public void setOriginalGoal(Goal goal);

	/**
	 * @param newName -
	 *            the name for the new goal. if this is not set, or is set to
	 *            a name already in use, a unique name will be generated.
	 */
	public void setNewGoalName(String newName);

	/**
	 * @return the new copy of the use case.
	 */
	public Goal getNewGoal();
}
