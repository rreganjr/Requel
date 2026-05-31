/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.assistant.legacynlp;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.NLPText;
import com.rreganjr.nlp.dictionary.SemanticRole;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollector;
import com.rreganjr.nlp.impl.srl.SemanticRoleCollectorFunction;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;

/**
 * SPI adapter reproducing the legacy {@code ScenarioStepAssistant} step-structure
 * analysis as {@link AnnotationAction}s. A step's name is expected to read like
 * "&lt;actor&gt; does something"; the assistant uses semantic-role labelling to
 * find the agent (subject) and then:
 * <ul>
 * <li>if the subject matches a known actor, checks that the actor is associated
 * with every use case that (transitively) uses the step's scenarios, raising an
 * issue per use case where it is not;</li>
 * <li>if the subject does not match a known actor, raises a "subject doesn't
 * match a known actor" issue;</li>
 * <li>if no agent could be identified, adds a note that the step could not be
 * analyzed.</li>
 * </ul>
 * Issues are general (non-lexical) and each carries a "Do nothing." position,
 * matching the legacy {@code addSimpleIssue} behaviour.
 *
 * <p>
 * Notes (issue #43, Phase 4.5 Step 4d):
 * <ul>
 * <li>Targets {@link Step}; {@link Scenario} extends {@code Step}, so a scenario
 * edit analyses the scenario itself. Per the SPI per-target model the legacy
 * cascade to a scenario's child steps is not reproduced — each step is analysed
 * when it is itself edited.</li>
 * <li>The legacy transitive-scenario walk mutated the collection it was iterating
 * (a latent {@code ConcurrentModificationException}); this uses a visited-set
 * worklist instead.</li>
 * </ul>
 */
@Component
public class StepStructureAssistant implements RequelAssistant<Step> {

	private static final Logger log = LoggerFactory.getLogger(StepStructureAssistant.class);

	public static final String ASSISTANT_ID = "legacy-step-structure";

	private static final String DO_NOTHING_MSG = "Do nothing.";
	private static final String COULD_NOT_ANALYZE_STEP_MSG =
			"The assistant could not analyze the structure of the step text. It may be too complex "
					+ "or didn't have an identifiable syntactic subject.";
	private static final String SUBJECT_DOESNT_MATCH_ACTOR_MSG =
			"The subject of the step text \"{0}\" does not match a known actor.";
	private static final String ACTOR_NOT_IN_USECASE_MSG =
			"The actor of the step \"{0}\" is not associated to the use case \"{1}\".";

	private final NLPProcessorFactory nlpProcessorFactory;

	@Autowired
	public StepStructureAssistant(NLPProcessorFactory nlpProcessorFactory) {
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<Step> targetType() {
		return Step.class;
	}

	@Override
	public AssistantResult analyze(AssistantContext context, Step step) {
		String entityType = step.getProjectOrDomainEntityInterface().getSimpleName();
		EntityRef targetRef = EntityRef.of(entityType, step.getId());
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Step-structure analysis");
		try {
			analyzeStep(builder, targetRef, step);
		} catch (RuntimeException e) {
			log.warn("step-structure analysis of {} failed; skipping: {}", targetRef, e.toString());
		}
		return builder.build();
	}

	private void analyzeStep(AssistantResult.Builder builder, EntityRef targetRef, Step step) {
		String name = step.getName();
		if (name == null || name.isBlank()) {
			return;
		}
		NLPText text = nlpProcessorFactory.processText(name);
		Map<SemanticRole, NLPText> roles = new SemanticRoleCollector(
				new SemanticRoleCollectorFunction(text.getPrimaryVerb())).process(text);
		NLPText agentText = roles.get(SemanticRole.AGENT);

		if (agentText == null || agentText.getText().isEmpty()) {
			builder.annotationAction(note(targetRef, "could-not-analyze",
					COULD_NOT_ANALYZE_STEP_MSG));
			return;
		}

		String subject = agentText.getText();
		boolean matchesExistingActor = false;
		for (Actor actor : step.getProjectOrDomain().getActors()) {
			if (subject.equals(actor.getName())) {
				matchesExistingActor = true;
				checkActorUseCaseAssociations(builder, targetRef, step, actor);
			}
		}
		if (!matchesExistingActor) {
			String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
					+ ":subject-not-actor:" + subject;
			emitGeneralIssue(builder, targetRef, key,
					MessageFormat.format(SUBJECT_DOESNT_MATCH_ACTOR_MSG, subject), "subject-not-actor",
					subject);
		}
	}

	private void checkActorUseCaseAssociations(AssistantResult.Builder builder, EntityRef targetRef,
			Step step, Actor actor) {
		Deque<Scenario> toVisit = new ArrayDeque<>(step.getUsingScenarios());
		Set<Scenario> visited = new HashSet<>();
		while (!toVisit.isEmpty()) {
			Scenario scenario = toVisit.poll();
			if (scenario == null || !visited.add(scenario)) {
				continue;
			}
			toVisit.addAll(scenario.getUsingScenarios());
			for (UseCase useCase : scenario.getUsingUseCases()) {
				if (!useCase.getActors().contains(actor)
						&& !actor.equals(useCase.getPrimaryActor())) {
					String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":"
							+ targetRef.entityId() + ":actor-not-in-usecase:" + actor.getName() + ":"
							+ useCase.getName();
					emitGeneralIssue(builder, targetRef, key,
							MessageFormat.format(ACTOR_NOT_IN_USECASE_MSG, actor.getName(),
									useCase.getName()),
							"actor-not-in-usecase", actor.getName());
				}
			}
		}
	}

	private void emitGeneralIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String issueKey, String text, String findingType, String snippet) {
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofSnippet(snippet));
		builder.annotationAction(new AnnotationAction(issueKey,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, null, null,
				evidence, Map.of("mustResolve", Boolean.TRUE, "findingType", findingType)));
		builder.annotationAction(new AnnotationAction(issueKey + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, issueKey, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}

	private AnnotationAction note(EntityRef targetRef, String findingType, String text) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId() + ":"
				+ findingType;
		return new AnnotationAction(key, AnnotationAction.ActionType.CREATE_OR_UPDATE_NOTE, targetRef,
				null, text, null, null, List.of(EvidenceRef.ofLocator("property=Name")),
				Map.of("findingType", findingType));
	}
}
