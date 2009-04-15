/*
 * $Id: ScenarioStepAssistant.java,v 1.9 2009/02/09 10:12:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project.impl.assistant;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.impl.srl.SemanticRoleCollector;
import edu.harvard.fas.rregan.nlp.impl.srl.SemanticRoleCollectorFunction;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Analyses a scenario step and adds annotations with suggestions.
 * 
 * @author ron
 */
public class ScenarioStepAssistant extends TextEntityAssistant {
	private static final Logger log = Logger.getLogger(ProjectOrDomainEntityAssistant.class);

	/**
	 * The name of the property in the ScenarioStepAssistant.properties file for
	 * the note text indicating the assistant couldn't analyze the structure of
	 * the step.
	 */
	public static final String PROP_COULD_NOT_ANALYZE_STEP_MSG = "CouldNotAnalyzeStepMessage";
	public static final String PROP_COULD_NOT_ANALYZE_STEP_MSG_DEFAULT = "The assistant could not analyze the structure of the step text. It may be too complex or didn't have an identifiable syntactic subject.";

	/**
	 * The name of the property in the ScenarioStepAssistant.properties file for
	 * the issue text indicating the subject of the step doesn't match a known
	 * actor.
	 */
	public static final String PROP_SUBJECT_DOESNT_MATCH_ACTOR_MSG = "SubjectDoesntMatchActorMessage";
	public static final String PROP_SUBJECT_DOESNT_MATCH_ACTOR_MSG_DEFAULT = "The subject of the step text \"{0}\" does not match a known actor.";

	/**
	 * The name of the property in the ScenarioStepAssistant.properties file for
	 * the issue text indicating the assistant couldn't find.
	 */
	public static final String PROP_ACTOR_NOT_IN_USECASE_MSG = "ActorNotInUseCase";
	public static final String PROP_ACTOR_NOT_IN_USECASE_MSG_DEFAULT = "The actor of the step \"{0}\" is not associated to the use case \"{1}\".";

	/**
	 * @param resourceBundleName -
	 *            the full class name to use for the resource bundle.
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	protected ScenarioStepAssistant(String resourceBundleName, LexicalAssistant lexicalAssistant,
			User assistantUser) {
		super(resourceBundleName, lexicalAssistant, assistantUser);
	}

	/**
	 * @param step
	 */
	@Override
	public void analyze() {
		super.analyze(); // analyze name and text
		if (getEntity() instanceof Step) {
			analyzeStep((Step) getEntity());
		}
	}

	private void analyzeStep(Step step) {
		// The step name should be of the form "<actor> does something"
		// identify the actor
		NLPText text = getPropertyNlpText(PROP_NAME);
		Map<SemanticRole, NLPText> roles = new SemanticRoleCollector(
				new SemanticRoleCollectorFunction(text.getPrimaryVerb())).process(text);
		NLPText agentText = roles.get(SemanticRole.AGENT);
		if ((agentText != null) && (agentText.getText().length() > 0)) {
			String subject = agentText.getText();

			boolean matchesExistingActor = false;
			for (Actor actor : step.getProjectOrDomain().getActors()) {
				if (subject.equals(actor.getName())) {
					matchesExistingActor = true;
					Queue<Scenario> scenarios = new LinkedList<Scenario>(step.getUsingScenarios());
					for (Scenario scenario : scenarios) {
						scenarios.addAll(scenario.getUsingScenarios());
						for (UseCase useCase : scenario.getUsingUseCases()) {
							if (!useCase.getActors().contains(actor)
									&& !actor.equals(useCase.getPrimaryActor())) {
								try {
									addSimpleIssue(step.getProjectOrDomain(), getAssistantUser(),
											step, createMessage(PROP_ACTOR_NOT_IN_USECASE_MSG,
													PROP_ACTOR_NOT_IN_USECASE_MSG_DEFAULT, actor
															.getName(), useCase.getName()));
								} catch (Exception e2) {
									log.error("failed to add note indicating actor"
											+ " isn't associated with the usecase.", e2);
								}
							}
						}
					}
				}

			}
			if (!matchesExistingActor) {
				try {
					addSimpleIssue(step.getProjectOrDomain(), getAssistantUser(), step,
							createMessage(PROP_SUBJECT_DOESNT_MATCH_ACTOR_MSG,
									PROP_SUBJECT_DOESNT_MATCH_ACTOR_MSG_DEFAULT, subject));
				} catch (Exception e2) {
					log.error("failed to add note indicating step subject is not an actor.", e2);
				}
			}
		} else {
			try {
				addNote(step.getProjectOrDomain(), getAssistantUser(), step, createMessage(
						PROP_COULD_NOT_ANALYZE_STEP_MSG, PROP_COULD_NOT_ANALYZE_STEP_MSG_DEFAULT));
			} catch (Exception e2) {
				log.error("failed to add note indicating failure of step analysis.", e2);
			}
		}
	}
}
