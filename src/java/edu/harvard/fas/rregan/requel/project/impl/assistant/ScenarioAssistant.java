/*
 * $Id: ScenarioAssistant.java,v 1.4 2009/01/23 09:54:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.user.User;

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
