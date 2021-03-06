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

import com.rreganjr.requel.command.EditCommand;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;

/**
 * @author ron
 */
public interface AddGoalToGoalContainerCommand extends EditCommand {

	/**
	 * Set the goal to add.
	 * 
	 * @param goal
	 */
	public void setGoal(Goal goal);

	/**
	 * @return the updated goal.
	 */
	public Goal getGoal();

	/**
	 * Set the container this goal is being added to.
	 * 
	 * @param goalContainer
	 */
	public void setGoalContainer(GoalContainer goalContainer);

	/**
	 * @return the updated goal container.
	 */
	public GoalContainer getGoalContainer();
}
