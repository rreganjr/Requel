/*
 * $Id: UseCaseAssistant.java,v 1.6 2009/01/23 09:54:23 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
