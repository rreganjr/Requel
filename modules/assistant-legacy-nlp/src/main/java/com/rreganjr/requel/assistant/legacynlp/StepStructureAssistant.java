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
import com.rreganjr.requel.project.StepNames;
import com.rreganjr.requel.project.ActorRole;
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
	/** #459/#468: one primary actor; {1} how the step reads with it, {2} with the system. */
	static final String NO_ACTOR_ACTOR_MSG = "The step \"{0}\" doesn''t say who does it. Set its"
			+ " actor: as the primary actor it reads \"{1}\", or as the system \"{2}\".";
	/** #459/#468: use cases with different primary actors share the step; {1} lists them. */
	static final String NO_ACTOR_SHARED_MSG = "The step \"{0}\" doesn''t say who does it, and the"
			+ " use cases that share it have different primary actors ({1}). Set its actor to the"
			+ " primary actor, so it reads with each use case''s own, or to the system: \"{2}\".";
	/** #459/#468: the step is in no use case; the primary-actor role reads as the role. */
	static final String NO_ACTOR_NO_USE_CASE_MSG = "The step \"{0}\" doesn''t say who does it,"
			+ " and it isn''t in a use case. Set its actor to the primary actor, so it reads"
			+ " \"{1}\" until it joins one, or to the system.";
	/** #468: the positions that set a step's actor role; {0} how it then reads. */
	static final String SET_PRIMARY_MSG = "Set the actor to the primary actor: \"{0}\".";
	static final String SET_SYSTEM_MSG = "Set the actor to the system: \"{0}\".";
	static final String SET_NAMED_MSG = "Set the actor to {1}: \"{0}\".";
	/**
	 * #468: a step names an actor that is the primary actor of some of its use cases but not of
	 * others ({2}); {3} is the action it would keep as its name.
	 */
	static final String USE_ROLE_MSG = "\"{0}\" names {1}, the primary actor of some of its use"
			+ " cases but not of {2}. Set its actor to the primary actor so it reads with each use"
			+ " case''s own, or to {1}, and keep \"{3}\" as its name.";
	/** #468: adds the actor a step names to a use case. */
	static final String ADD_TO_USE_CASE_MSG = "Add \"{0}\" to the use case \"{1}\".";
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
		// #468: a step with an actor role stores only the action; its actor isn't in its wording.
		if (step.getActorRole() != null) {
			if (step.getActorRole() == ActorRole.NAMED && step.getActor() != null) {
				checkActorUseCaseAssociations(builder, targetRef, step, step.getActor());
			}
			return;
		}
		StepReading reading = StepReading.withKnownSubjects(
				StepReading.read(nlpProcessorFactory, name), name, actorNames(step));
		if (reading.kind() == StepReading.Kind.UNREADABLE) {
			emitAdvisoryIssue(builder, targetRef, "unreadable", MessageFormat.format(
					scenario ? UNREADABLE_SCENARIO_MSG : UNREADABLE_STEP_MSG, name), name);
			return;
		}
		if (reading.kind() == StepReading.Kind.COMMAND) {
			// #349: a command. Fine for a scenario's name; a step should say who does it.
			if (!scenario) {
				emitNoActorIssue(builder, targetRef, step, name, reading);
			}
			return;
		}

		String subject = reading.subject();
		if (isSystem(subject)) {
			return;
		}
		Actor matched = null;
		for (Actor actor : step.getProjectOrDomain().getActors()) {
			if (namesActor(subject, actor.getName())) {
				if (matched == null) {
					matched = actor;
				}
				checkActorUseCaseAssociations(builder, targetRef, step, actor);
			}
		}
		if (matched != null) {
			emitUseRoleIssue(builder, targetRef, step, name, reading, matched);
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

	/** #486: the names of the project's actors, for {@link StepReading#withKnownSubjects}. */
	static List<String> actorNames(Step step) {
		if (step.getProjectOrDomain() == null) {
			return List.of();
		}
		return step.getProjectOrDomain().getActors().stream().map(Actor::getName)
				.filter(java.util.Objects::nonNull).toList();
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
				List<EvidenceRef> evidence = List.of(EvidenceRef.ofSnippet(actor.getName()));
				builder.annotationAction(new AnnotationAction(key,
						AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
						MessageFormat.format(ACTOR_NOT_IN_USECASE_MSG, actor.getName(),
								useCase.getName()), null, null, evidence,
						Map.of("mustResolve", Boolean.TRUE, "findingType", "actor-not-in-usecase")));
				// #468: one click adds the actor to the use case.
				builder.annotationAction(new AnnotationAction(key + ":add-to-use-case",
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
						MessageFormat.format(ADD_TO_USE_CASE_MSG, actor.getName(), useCase.getName()),
						null, null, evidence, Map.of("kind", "ADD_ACTOR_TO_USE_CASE", "actorName",
								actor.getName(), "useCaseName", useCase.getName())));
				builder.annotationAction(new AnnotationAction(key + ":do-nothing",
						AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
						DO_NOTHING_MSG, null, null, evidence, Map.of()));
			}
		}
	}

	/**
	 * The use cases a step is in, by name: those whose primary or additional scenarios contain
	 * it, directly or through scenarios used as steps. #459: additional scenarios count too;
	 * {@code Scenario.getUsingUseCases()} covers only the use cases a scenario is primary in.
	 */
	static List<UseCase> useCasesOf(Step step) {
		return StepNames.useCasesContaining(step);
	}

	/**
	 * #455: the use cases a scenario is in, by name: those whose primary or additional scenarios
	 * are the scenario itself or contain it through scenarios used as steps.
	 */
	static List<UseCase> useCasesOfScenario(Scenario scenario) {
		return StepNames.useCasesOfScenario(scenario);
	}

	/**
	 * #459/#468: a step written as a command doesn't say who does it. Each position gives it an
	 * actor role and keeps the action ("ends the room") as its name:
	 * <ul>
	 * <li>One primary actor across the step's use cases: the primary actor, each additional actor
	 * of those use cases, and the system.</li>
	 * <li>Use cases with different primary actors share the step: the primary actor, which reads
	 * with each use case's own (#459 could offer only the system here), and the system.</li>
	 * <li>No use case: the primary actor, which reads "Primary actor ..." until the step joins a
	 * use case, and the system.</li>
	 * </ul>
	 * When the action can't be worked out from the command, the issue has no positions.
	 * Keyed by finding type as before, so a re-run updates the issue in place; the applicator
	 * replaces the positions a re-run no longer offers (#459's name rewrites among them).
	 */
	private void emitNoActorIssue(AssistantResult.Builder builder, EntityRef targetRef, Step step,
			String name, StepReading reading) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":no-actor";
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=name"),
				EvidenceRef.ofSnippet(name));
		String action = StepVerbs.predicate(name, reading);
		List<UseCase> useCases = useCasesOf(step);
		List<RoleOption> options = new ArrayList<>();
		String text;
		if (action == null || action.length() > MAX_NAME_LENGTH) {
			text = MessageFormat.format(NO_ACTOR_MSG, name);
		} else {
			String systemReads = StepNames.SYSTEM_LABEL + " " + action;
			List<Actor> primaries = useCases.stream().map(UseCase::getPrimaryActor)
					.filter(java.util.Objects::nonNull).distinct()
					.sorted(Comparator.comparing(Actor::getName,
							Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
					.toList();
			if (useCases.isEmpty()) {
				String reads = StepNames.PRIMARY_ACTOR_LABEL + " " + action;
				text = MessageFormat.format(NO_ACTOR_NO_USE_CASE_MSG, name, reads);
				options.add(RoleOption.primary(reads));
			} else if (primaries.size() == 1) {
				Actor primary = primaries.get(0);
				String reads = primary.getName() + " " + action;
				text = MessageFormat.format(NO_ACTOR_ACTOR_MSG, name, reads, systemReads);
				options.add(RoleOption.primary(reads));
				useCases.stream().flatMap(useCase -> useCase.getActors().stream())
						.filter(actor -> !actor.equals(primary) && actor.getName() != null)
						.distinct()
						.sorted(Comparator.comparing(Actor::getName, String.CASE_INSENSITIVE_ORDER))
						.forEach(actor -> options.add(RoleOption.named(actor.getName(),
								actor.getName() + " " + action)));
			} else {
				String shared = useCases.stream()
						.map(useCase -> useCase.getName() + ": " + (useCase.getPrimaryActor() == null
								? "no primary actor" : useCase.getPrimaryActor().getName()))
						.collect(java.util.stream.Collectors.joining("; "));
				text = MessageFormat.format(NO_ACTOR_SHARED_MSG, name, shared, systemReads);
				options.add(RoleOption.primary(StepNames.PRIMARY_ACTOR_LABEL + " " + action));
			}
			options.add(RoleOption.system(systemReads));
		}
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType", "no-actor")));
		emitRoleOptions(builder, key, evidence, name, action, options);
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}

	/**
	 * #468: a step whose wording names an actor that is the primary actor of some of its use cases
	 * but not of the others reads wrong in those others ("Operator ends the room" in an Engineer
	 * use case). A LOW advisory with two positions: the primary-actor role, which reads with each
	 * use case's own, and the actor by name. Steps whose wording is right everywhere get nothing,
	 * so a well-written step carries no open issue. Nothing when the action can't be read off it.
	 */
	private void emitUseRoleIssue(AssistantResult.Builder builder, EntityRef targetRef, Step step,
			String name, StepReading reading, Actor actor) {
		List<UseCase> useCases = Scenario.class.equals(step.getProjectOrDomainEntityInterface())
				? StepNames.useCasesOf(step) : useCasesOf(step);
		List<UseCase> elsewhere = useCases.stream()
				.filter(useCase -> !actor.equals(useCase.getPrimaryActor())).toList();
		if (elsewhere.isEmpty() || elsewhere.size() == useCases.size()) {
			return;
		}
		String action = StepVerbs.predicate(name, reading);
		if (action == null || action.length() > MAX_NAME_LENGTH) {
			return;
		}
		List<RoleOption> options = List.of(
				RoleOption.primary(StepNames.PRIMARY_ACTOR_LABEL + " " + action),
				RoleOption.named(actor.getName(), actor.getName() + " " + action));
		String others = elsewhere.stream().map(useCase -> "\"" + useCase.getName() + "\"")
				.collect(java.util.stream.Collectors.joining(", "));
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":use-actor-role";
		List<EvidenceRef> evidence = List.of(EvidenceRef.ofLocator("property=name"),
				EvidenceRef.ofSnippet(name));
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null,
				MessageFormat.format(USE_ROLE_MSG, name, actor.getName(), others, action), "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType",
						"use-actor-role")));
		emitRoleOptions(builder, key, evidence, name, action, options);
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}

	/** #468: one SET_STEP_ACTOR position per option, after the issue and before "Do nothing.". */
	private static void emitRoleOptions(AssistantResult.Builder builder, String key,
			List<EvidenceRef> evidence, String name, String action, List<RoleOption> options) {
		for (RoleOption option : options) {
			if (option.reads().length() > MAX_NAME_LENGTH) {
				continue;
			}
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("kind", "SET_STEP_ACTOR");
			metadata.put("proposedName", action);
			metadata.put("expectedName", name);
			metadata.put("actorRole", option.role().name());
			if (option.actorName() != null) {
				metadata.put("actorName", option.actorName());
			}
			String text = switch (option.role()) {
			case PRIMARY_ACTOR -> MessageFormat.format(SET_PRIMARY_MSG, option.reads());
			case SYSTEM -> MessageFormat.format(SET_SYSTEM_MSG, option.reads());
			case NAMED -> MessageFormat.format(SET_NAMED_MSG, option.reads(), option.actorName());
			};
			String suffix = switch (option.role()) {
			case PRIMARY_ACTOR -> "primary";
			case SYSTEM -> "system";
			case NAMED -> "actor:" + option.actorName();
			};
			builder.annotationAction(new AnnotationAction(key + ":set-actor:" + suffix,
					AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, text, null,
					null, evidence, metadata));
		}
	}

	/** #468: a role a position can give a step, and how the step then reads. */
	private record RoleOption(ActorRole role, String actorName, String reads) {
		static RoleOption primary(String reads) {
			return new RoleOption(ActorRole.PRIMARY_ACTOR, null, reads);
		}

		static RoleOption system(String reads) {
			return new RoleOption(ActorRole.SYSTEM, null, reads);
		}

		static RoleOption named(String actorName, String reads) {
			return new RoleOption(ActorRole.NAMED, actorName, reads);
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
