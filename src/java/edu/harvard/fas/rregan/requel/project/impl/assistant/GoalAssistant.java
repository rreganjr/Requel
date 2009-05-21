/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
