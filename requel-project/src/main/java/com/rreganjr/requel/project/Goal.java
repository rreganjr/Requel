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
package com.rreganjr.requel.project;

import java.util.Set;

/**
 * A Project Entity representing desired properties, constraints, features and
 * functions of the system being defined. Goals are primarily textual with
 * relationships between them indicating a goal supports or conflicts with
 * another goal.
 * 
 * @author ron
 */
public interface Goal extends TextEntity, Comparable<Goal> {

	/**
	 * The relationships that this goal has with other goals.
	 * 
	 * @return The GoalRelations that originate from this goal
	 */
	public Set<GoalRelation> getRelationsFromThisGoal();

	/**
	 * The relationships that other goals have with this goal.
	 * 
	 * @return The GoalRElations that terminate at this goal
	 */
	public Set<GoalRelation> getRelationsToThisGoal();

	/**
	 * Return the project entities that have direct associations to this goal.
	 * 
	 * @return
	 */
	public Set<GoalContainer> getReferrers();
}
