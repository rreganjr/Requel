/*
 * $Id: Goal.java,v 1.11 2008/09/07 03:46:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

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
	public Set<GoalContainer> getReferers();
}
