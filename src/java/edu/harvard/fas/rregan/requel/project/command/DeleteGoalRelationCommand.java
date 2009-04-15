/*
 * $Id: DeleteGoalRelationCommand.java,v 1.1 2008/11/05 18:25:52 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.EditCommand;
import edu.harvard.fas.rregan.requel.project.GoalRelation;

/**
 * Delete a goal relationship between two goals.
 * @author ron
 */
public interface DeleteGoalRelationCommand extends EditCommand {

	/**
	 * set the goal relation to delete.
	 * @param goalRelation
	 */
	public void setGoalRelation(GoalRelation goalRelation);
}
