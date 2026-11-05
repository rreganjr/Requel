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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
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
 * analysis as {@link AnnotationAction}s. A use case and its scenarios are meant to be well
 * structured (a less formal description is a story), so the assistant checks how a step's or a
 * scenario's name reads, using the primary verb and semantic-role labelling (issue #349):
 * <ul>
 * <li>A <b>step</b> reads "&lt;actor&gt; does something" or "the system does something" (the
 * system is the implicit actor responding to a user action). A command ("Start the livestream")
 * raises an advisory "doesn't say who does it" issue.</li>
 * <li>A <b>scenario name</b> reads "&lt;actor&gt; does something" or is a command (a primary
 * scenario is usually named after its use case's goal phrase), so a command raises nothing.</li>
 * <li>For both, a subject that is a known actor is checked against every use case that
 * (transitively) uses the step's scenarios, raising an issue per use case it is not associated
 * with; "the system" is accepted with no actor; the project's own name raises an issue offering
 * to write "the system" instead or to add it as an actor; any other subject raises a "subject
 * doesn't match a known actor" issue.</li>
 * <li>Text with no verb found at all ("Webinar", a bracketed note, or a sentence the parser got
 * wrong) raises an advisory "couldn't read this" issue. It replaced the "could not analyze" note,
 * which didn't tell a step with no actor from text that couldn't be read.</li>
 * </ul>
 * Advisory issues are LOW and need not be resolved; every issue carries a "Do nothing." position,
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
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class StepStructureAssistant implements RequelAssistant<Step> {

	private static final Logger log = LoggerFactory.getLogger(StepStructureAssistant.class);

	public static final String ASSISTANT_ID = "legacy-step-structure";

	private static final String DO_NOTHING_MSG = "Do nothing.";
	/** #349: when the step's verb can't be found to build an example from. */
	static final String NO_ACTOR_MSG = "The step \"{0}\" doesn''t say who does it. Name the actor,"
			+ " as in \"<actor> does something\", or write \"The system ...\" when the system"
			+ " responds.";
	/** #459: one primary actor; {1} the step rewritten for it, {2} for the system. */
	static final String NO_ACTOR_ACTOR_MSG = "The step \"{0}\" doesn''t say who does it. Name the"
			+ " actor, for example \"{1}\", or \"{2}\" when the system does it.";
	/** #459: use cases with different primary actors share the step; {1} lists them. */
	static final String NO_ACTOR_SHARED_MSG = "The step \"{0}\" doesn''t say who does it, and the"
			+ " use cases that share it have different primary actors ({1}). Split the step, or"
			+ " name the actor, for example \"{2}\" when the system does it.";
	/** #459: the step is in no use case, so there is no actor to name. */
	static final String NO_ACTOR_NO_USE_CASE_MSG = "The step \"{0}\" doesn''t say who does it,"
			+ " and it isn''t in a use case. Name the actor, for example \"{1}\".";
	static final String REWRITE_MSG = "Rewrite as \"{0}\".";
	/** #459: the longest name a rewrite may propose (ValidationLimits.ARTIFACT_NAME_MAX). */
	static final int MAX_NAME_LENGTH = 255;
	static final String UNREADABLE_STEP_MSG = "Requel couldn''t read \"{0}\" as \"<actor> does"
			+ " something\" or a command. A step names who does it.";
	static final String UNREADABLE_SCENARIO_MSG = "Requel couldn''t read \"{0}\" as \"<actor> does"
			+ " something\" or a command.";
	static final String PROJECT_AS_ACTOR_MSG = "\"{0}\" names the project as its actor. Write"
			+ " \"The system\" when the system does it, or add \"{1}\" as an actor.";
	static final String REPLACE_WITH_SYSTEM_MSG = "Replace \"{0}\" with \"{1}\".";
	static final String ADD_AS_ACTOR_MSG = "Add \"{0}\" as an actor.";
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
		boolean scenario = Scenario.class.equals(step.getProjectOrDomainEntityInterface());
		StepReading reading = StepReading.read(nlpProcessorFactory, name);
		if (reading.kind() == StepReading.Kind.UNREADABLE) {
			emitAdvisoryIssue(builder, targetRef, "unreadable", MessageFormat.format(
					scenario ? UNREADABLE_SCENARIO_MSG : UNREADABLE_STEP_MSG, name), name);
			return;
		}
		if (reading.kind() == StepReading.Kind.COMMAND) {
			// #349: a command. Fine for a scenario's name; a step should say who does it.
			if (!scenario) {
				emitNoActorIssue(builder, targetRef, step, name, reading.verb());
			}
			return;
		}

		String subject = reading.subject();
		if (isSystem(subject)) {
			return;
		}
		boolean matchesExistingActor = false;
		for (Actor actor : step.getProjectOrDomain().getActors()) {
			if (namesActor(subject, actor.getName())) {
				matchesExistingActor = true;
				checkActorUseCaseAssociations(builder, targetRef, step, actor);
			}
		}
		if (matchesExistingActor) {
			return;
		}
		String projectName = projectNameIn(subject, step.getProjectOrDomain().getName());
		if (projectName != null) {
			emitProjectAsActorIssue(builder, targetRef, name, subject, projectName);
			return;
		}
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":subject-not-actor:" + subject;
		emitGeneralIssue(builder, targetRef, key,
				MessageFormat.format(SUBJECT_DOESNT_MATCH_ACTOR_MSG, subject), "subject-not-actor",
				subject);
	}

	/**
	 * #349: whether the subject names the actor: "The host" names the actor Host. A leading "the",
	 * "a" or "an" and case are ignored (an exact match used to be required, so "The host" never
	 * matched).
	 */
	static boolean namesActor(String subject, String actorName) {
		return actorName != null && withoutArticle(subject).equalsIgnoreCase(withoutArticle(actorName));
	}

	static String withoutArticle(String text) {
		String s = text.trim();
		for (String article : new String[] { "the ", "a ", "an " }) {
			if (s.regionMatches(true, 0, article, 0, article.length())) {
				return s.substring(article.length()).trim();
			}
		}
		return s;
	}

	/** #349: "system" or "the system", ignoring case - the implicit actor. */
	static boolean isSystem(String subject) {
		String s = subject.trim().toLowerCase(Locale.ROOT);
		return "system".equals(s) || "the system".equals(s);
	}

	/**
	 * #349: the project name as the subject was written ("Roundtable" from "the Roundtable"), or
	 * null when the subject isn't the project's name (ignoring case and a leading "the").
	 */
	static String projectNameIn(String subject, String projectName) {
		if (projectName == null || projectName.isBlank()) {
			return null;
		}
		String s = subject.trim();
		if (s.regionMatches(true, 0, "the ", 0, 4)) {
			s = s.substring(4).trim();
		}
		return s.equalsIgnoreCase(projectName.trim()) ? s : null;
	}

	/**
	 * #349: the project's name used as the actor. A lexical issue on the name (its word the name
	 * as written) with one-click positions to write "the system" instead (Fix Spelling) or to add
	 * the name as an actor, and "Do nothing."; as strong as the subject-not-actor issue it
	 * replaces.
	 */
	private void emitProjectAsActorIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String name, String subject, String projectName) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":project-as-actor:" + projectName;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=name"),
				EvidenceRef.ofSnippet(subject));
		// "the Roundtable" keeps its article ("the system"); a bare "Roundtable" gets one, and
		// Fix Spelling carries the leading capital over.
		String replacement = subject.equals(projectName) ? "the system" : "system";
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(PROJECT_AS_ACTOR_MSG, name, projectName), "MEDIUM", null,
				evidence, Map.of("kind", "LEXICAL", "word", projectName,
						"annotatableEntityPropertyName", "Name", "mustResolve", Boolean.TRUE,
						"findingType", "project-as-actor")));
		builder.annotationAction(new AnnotationAction(key + ":the-system",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
				MessageFormat.format(REPLACE_WITH_SYSTEM_MSG, projectName, replacement), null, null,
				evidence, Map.of("kind", "CHANGE_SPELLING", "proposedWord", replacement)));
		builder.annotationAction(new AnnotationAction(key + ":add-actor",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
				MessageFormat.format(ADD_AS_ACTOR_MSG, projectName), null, null, evidence,
				Map.of("kind", "ADD_ACTOR_TO_PROJECT")));
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}

	private void checkActorUseCaseAssociations(AssistantResult.Builder builder, EntityRef targetRef,
			Step step, Actor actor) {
		for (UseCase useCase : useCasesOf(step)) {
			if (!useCase.getActors().contains(actor) && !actor.equals(useCase.getPrimaryActor())) {
				String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
						+ ":actor-not-in-usecase:" + actor.getName() + ":" + useCase.getName();
				emitGeneralIssue(builder, targetRef, key,
						MessageFormat.format(ACTOR_NOT_IN_USECASE_MSG, actor.getName(),
								useCase.getName()),
						"actor-not-in-usecase", actor.getName());
			}
		}
	}

	/**
	 * The use cases a step is in, by name: those whose primary or additional scenarios contain
	 * it, directly or through scenarios used as steps. #459: additional scenarios count too;
	 * {@code Scenario.getUsingUseCases()} covers only the use cases a scenario is primary in.
	 */
	static List<UseCase> useCasesOf(Step step) {
		return useCasesReachedFrom(step.getUsingScenarios(), step);
	}

	/**
	 * #455: the use cases a scenario is in, by name: those whose primary or additional scenarios
	 * are the scenario itself or contain it through scenarios used as steps.
	 */
	static List<UseCase> useCasesOfScenario(Scenario scenario) {
		return useCasesReachedFrom(List.of(scenario), scenario);
	}

	private static List<UseCase> useCasesReachedFrom(java.util.Collection<Scenario> start,
			Step step) {
		Deque<Scenario> toVisit = new ArrayDeque<>(start);
		Set<Scenario> containing = new HashSet<>();
		while (!toVisit.isEmpty()) {
			Scenario scenario = toVisit.poll();
			if (scenario == null || !containing.add(scenario)) {
				continue;
			}
			toVisit.addAll(scenario.getUsingScenarios());
		}
		Set<UseCase> useCases = new LinkedHashSet<>();
		for (Scenario scenario : containing) {
			useCases.addAll(scenario.getUsingUseCases());
		}
		if (!containing.isEmpty() && step.getProjectOrDomain() != null) {
			for (UseCase useCase : step.getProjectOrDomain().getUseCases()) {
				if (containing.contains(useCase.getScenario()) || useCase.getAdditionalScenarios()
						.stream().anyMatch(containing::contains)) {
					useCases.add(useCase);
				}
			}
		}
		List<UseCase> sorted = new ArrayList<>(useCases);
		sorted.sort(Comparator.comparing(UseCase::getName,
				Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return sorted;
	}

	/**
	 * #459: a step written as a command. The example is the step itself rewritten to name its
	 * actor, and each rewrite is a position that sets the step's name to it.
	 * <ul>
	 * <li>One primary actor across the step's use cases: rewrites for it, each additional actor
	 * and the system.</li>
	 * <li>Use cases with different primary actors share the step: only the system rewrite, since
	 * naming one actor makes a shared step wrong in the others.</li>
	 * <li>No use case: no rewrites; there is no actor to name.</li>
	 * </ul>
	 * Keyed by finding type as before, so a re-run updates the issue in place; the applicator
	 * replaces the rewrites a re-run no longer offers.
	 */
	private void emitNoActorIssue(AssistantResult.Builder builder, EntityRef targetRef, Step step,
			String name, String verb) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":no-actor";
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=name"),
				EvidenceRef.ofSnippet(name));
		String system = StepVerbs.rewrite(name, verb, "the system");
		List<UseCase> useCases = useCasesOf(step);
		Map<String, String> rewrites = new LinkedHashMap<>();
		String text;
		if (system == null) {
			text = MessageFormat.format(NO_ACTOR_MSG, name);
		} else if (useCases.isEmpty()) {
			text = MessageFormat.format(NO_ACTOR_NO_USE_CASE_MSG, name,
					StepVerbs.rewrite(name, verb, "<actor>"));
		} else {
			List<Actor> primaries = useCases.stream().map(UseCase::getPrimaryActor)
					.filter(java.util.Objects::nonNull).distinct()
					.sorted(Comparator.comparing(Actor::getName,
							Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
					.toList();
			if (primaries.size() == 1) {
				Actor primary = primaries.get(0);
				String example = StepVerbs.rewrite(name, verb, primary.getName());
				text = MessageFormat.format(NO_ACTOR_ACTOR_MSG, name, example, system);
				rewrites.put(primary.getName(), example);
				useCases.stream().flatMap(useCase -> useCase.getActors().stream())
						.filter(actor -> !actor.equals(primary) && actor.getName() != null)
						.distinct()
						.sorted(Comparator.comparing(Actor::getName, String.CASE_INSENSITIVE_ORDER))
						.forEach(actor -> rewrites.putIfAbsent(actor.getName(),
								StepVerbs.rewrite(name, verb, actor.getName())));
			} else {
				String shared = useCases.stream()
						.map(useCase -> useCase.getName() + ": " + (useCase.getPrimaryActor() == null
								? "no primary actor" : useCase.getPrimaryActor().getName()))
						.collect(java.util.stream.Collectors.joining("; "));
				text = MessageFormat.format(NO_ACTOR_SHARED_MSG, name, shared, system);
			}
			rewrites.put(null, system);
		}
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType", "no-actor")));
		for (Map.Entry<String, String> rewrite : rewrites.entrySet()) {
			String proposed = rewrite.getValue();
			if (proposed == null || proposed.length() > MAX_NAME_LENGTH) {
				continue;
			}
			String suffix = rewrite.getKey() == null ? "system" : "actor:" + rewrite.getKey();
			builder.annotationAction(new AnnotationAction(key + ":rewrite:" + suffix,
					AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
					MessageFormat.format(REWRITE_MSG, proposed), null, null, evidence,
					Map.of("kind", "REWRITE_NAME", "proposedName", proposed,
							"expectedName", name)));
		}
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
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

	/**
	 * #349: an advisory finding - a LOW issue that need not be resolved, with "Do nothing." -
	 * keyed by its finding type so a re-run updates it in place.
	 */
	private void emitAdvisoryIssue(AssistantResult.Builder builder, EntityRef targetRef,
			String findingType, String text, String snippet) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId() + ":"
				+ findingType;
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=name"),
				EvidenceRef.ofSnippet(snippet));
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType", findingType)));
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}
}
