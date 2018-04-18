/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.command;

import com.rreganjr.requel.user.AnalyzableEditCommand;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.ProjectOrDomain;

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
