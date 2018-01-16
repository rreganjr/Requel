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

import com.rreganjr.requel.CreatedEntity;
import com.rreganjr.requel.Describable;
import com.rreganjr.requel.annotation.Annotatable;

/**
 * A GoalRelation is a uni-directional relationship from one goal to another. A
 * goal may only have one relationship to another goal. Supported relationships
 * are defined by the GoalRelationshipType.
 * 
 * @author ron
 */
public interface GoalRelation extends Annotatable, Comparable<GoalRelation>, CreatedEntity,
		Describable {

	/**
	 * @return The goal that is the origin of the relationship.
	 */
	public Goal getFromGoal();

	/**
	 * @return The goal that is the target of the relationship.
	 */
	public Goal getToGoal();

	/**
	 * @return The type of relationship that the from goal has with the to goal.
	 */
	public GoalRelationType getRelationType();

}
