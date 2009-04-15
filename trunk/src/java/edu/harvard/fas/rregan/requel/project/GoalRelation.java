/*
 * $Id: GoalRelation.java,v 1.3 2008/11/06 00:52:48 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;

/**
 * A GoalRelation is a uni-directional relationship from one goal to another. A
 * goal may only have one relationship to another goal. Supported relationships
 * are defined by the GoalRelationshipType.
 * 
 * @author ron
 */
public interface GoalRelation extends Annotatable, Comparable<GoalRelation>, CreatedEntity, Describable {

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
