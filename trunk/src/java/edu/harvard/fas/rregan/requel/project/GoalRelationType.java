/*
 * $Id: GoalRelationType.java,v 1.2 2008/04/09 10:33:27 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * The type of relationship for a GoalRelation.
 * 
 * @author ron
 */
public enum GoalRelationType {

	/**
	 * Declares that the from goal has a positive influence on the success of
	 * the to goal.
	 */
	Supports,

	/**
	 * Declares that the from goal is incompatable with the to goal such that
	 * both can not be satisfied.
	 */
	Conflicts;

	private GoalRelationType() {
	}
}
