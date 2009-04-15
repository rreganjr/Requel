/*
 * $Id: GoalAssistant.java,v 1.31 2009/01/23 09:54:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Analyses goals and adds annotations with suggestions.
 * 
 * @author ron
 */
public class GoalAssistant extends TextEntityAssistant {

	/**
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public GoalAssistant(LexicalAssistant lexicalAssistant, User assistantUser) {
		super(GoalAssistant.class.getName(), lexicalAssistant, assistantUser);
	}

	/**
	 * @param goal
	 */
	@Override
	public void analyze() {
		super.analyze();
		if (getEntity() instanceof Goal) {
			analyzeGoalRelations((Goal) getEntity());
		}
	}

	protected void analyzeGoalRelations(Goal goal) {
		for (GoalRelation goalRelation : goal.getRelationsFromThisGoal()) {

		}
	}
}
