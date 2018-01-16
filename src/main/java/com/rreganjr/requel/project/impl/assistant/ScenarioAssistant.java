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
package com.rreganjr.requel.project.impl.assistant;

import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.user.User;

/**
 * Analyses a scenario and adds annotations with suggestions.
 * 
 * @author ron
 */
public class ScenarioAssistant extends ScenarioStepAssistant {

	/**
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param scenarioStepAssistant -
	 *            assistnat for analyzing the individual step text.
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public ScenarioAssistant(LexicalAssistant lexicalAssistant, User assistantUser) {
		super(ScenarioAssistant.class.getName(), lexicalAssistant, assistantUser);
	}

	/**
	 * @param scenario
	 */
	@Override
	public void analyze() {
		super.analyze(); // step level analysis
		if (getEntity() instanceof Scenario) {
			Scenario scenario = (Scenario) getEntity();
			for (Step step : scenario.getSteps()) {
				// NOTE: this resets the entity of this analyzer to a child step
				// or scenario. The original is saved in the local scenario
				// variable on the stack. the text of the scenario was already
				// analyzed by super.analyze
				setEntity(step);
				analyze();
			}
		}
	}
}
