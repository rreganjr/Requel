/*
 * $Id: UseCaseAssistant.java,v 1.6 2009/01/23 09:54:23 rregan Exp $
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

import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Analyses a use case and its elements and adds annotations with suggestions.
 * 
 * @author ron
 */
public class UseCaseAssistant extends TextEntityAssistant {

	private final ScenarioAssistant scenarioAssistant;
	private final ActorAssistant actorAssistant;

	/**
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param scenarioAssistant -
	 *            assistant for analyzing the use cases scenario.
	 * @param actorAssistant -
	 *            assistant for analyzing the primery actor.
	 * @param updatedEntityNotifier -
	 *            after an entity is analyzed it is passed to the notifier to
	 *            tell the UI components that reference the entity to refresh
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public UseCaseAssistant(LexicalAssistant lexicalAssistant, ScenarioAssistant scenarioAssistant,
			ActorAssistant actorAssistant, User assistantUser) {
		super(UseCaseAssistant.class.getName(), lexicalAssistant, assistantUser);
		this.scenarioAssistant = scenarioAssistant;
		this.actorAssistant = actorAssistant;
	}

	/**
	 * @param usecase
	 */
	@Override
	public void analyze() {
		super.analyze(); // analyze the name and text
		if (getEntity() instanceof UseCase) {
			UseCase useCase = (UseCase) getEntity();
			actorAssistant.setEntity(useCase.getPrimaryActor());
			actorAssistant.analyze();
			scenarioAssistant.setEntity(useCase.getScenario());
			scenarioAssistant.analyze();
		}
	}
}
