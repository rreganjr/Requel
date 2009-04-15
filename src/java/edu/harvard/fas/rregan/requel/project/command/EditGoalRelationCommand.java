/*
 * $Id: EditGoalRelationCommand.java,v 1.4 2009/02/13 12:08:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.command;

import edu.harvard.fas.rregan.requel.command.AnalyzableEditCommand;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;

/**
 * Create or Edit the relationship between two goals.
 * 
 * @author ron
 */
public interface EditGoalRelationCommand extends AnalyzableEditCommand {

	/**
	 * @param projectOrDomain -
	 *            the project or domain that the goals in the relation belong
	 *            to. this is required if goalRelation is null.
	 */
	public void setProjectOrDomain(ProjectOrDomain projectOrDomain);

	/**
	 * @param goalRelation -
	 *            the goal relation to edit, or null for a new relation.
	 */
	public void setGoalRelation(GoalRelation goalRelation);

	/**
	 * @return after execute this returns the created or edited relation.
	 */
	public GoalRelation getGoalRelation();

	/**
	 * @param goalName -
	 *            the name of the goal that is the origin of the relationship.
	 */
	public void setFromGoal(String goalName);

	/**
	 * @param goalName -
	 *            the name of the goal that is the target of the relationship.
	 */
	public void setToGoal(String goalName);

	/**
	 * @param relationTypeName -
	 *            the name of the type of relationship between the goals
	 */
	public void setRelationType(String relationTypeName);
}
