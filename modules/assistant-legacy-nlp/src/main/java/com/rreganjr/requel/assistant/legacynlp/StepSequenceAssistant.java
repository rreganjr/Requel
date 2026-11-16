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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.requel.assistant.api.AnnotationAction;
import com.rreganjr.requel.assistant.api.AssistantContext;
import com.rreganjr.requel.assistant.api.AssistantResult;
import com.rreganjr.requel.assistant.api.CleanupPolicy;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.EvidenceRef;
import com.rreganjr.requel.assistant.api.RequelAssistant;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AnalysisRequestSource;

/**
 * #455: whether a scenario's steps alternate between an actor and the system. #349 checks how one
 * step reads; this reads a scenario's own steps in order, each with #349's rules, and raises one
 * advisory LOW issue on the scenario listing every break:
 * <ul>
 * <li>the scenario starts with a system step;</li>
 * <li>two system steps in a row;</li>
 * <li>the same actor twice in a row, which is usually one step written as two. Different actors
 * in a row are a hand-off outside the system and raise nothing.</li>
 * </ul>
 * How a step counts: "the system ..." is the system; "&lt;actor&gt; ..." is that actor (a known
 * actor by name, otherwise the subject as written); a command is the primary actor of the
 * scenario's use cases when they have exactly one. A scenario used as a step counts the same way,
 * by its name. Anything else (unreadable, the project named as the actor, a command with no single
 * primary actor) is unknown and breaks the sequence: no pair is checked across it.
 * <p>
 * #472: each same-actor or two-system pair that can be merged into one step gets a position that
 * does it ("Merge into ..."), before "Leave the steps as they are." (#486).
 *
 * <p>
 * A separate assistant from {@link StepStructureAssistant}: stale findings are reconciled per
 * assistant and target, so a run that only re-checks the order ({@link #TASK_TYPE}, dispatched for
 * the scenarios that use an edited step) must not touch the step-structure findings on the
 * scenario's name. It auto-resolves an untouched issue once the order is fixed, and a project can
 * switch it off.
 */
@ConditionalOnProperty(name = "requel.nlp.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class StepSequenceAssistant implements RequelAssistant<Scenario> {

	private static final Logger log = LoggerFactory.getLogger(StepSequenceAssistant.class);

	public static final String ASSISTANT_ID = "legacy-step-sequence";
	/** The task of a run that only re-checks the step order. */
	public static final String TASK_TYPE = AnalysisRequestSource.STEP_SEQUENCE_TASK;
	static final String FINDING_TYPE = "step-sequence";

	/**
	 * #486: the sequence check's own wording. Positions are shared by their text in a project, so
	 * a plain "Do nothing." was the one the step-structure check created, and showed its credit.
	 */
	static final String DO_NOTHING_MSG = "Leave the steps as they are.";
	static final String ISSUE_MSG = "The steps of \"{0}\" don''t alternate between an actor and the"
			+ " system: {1}. An actor acts and the system responds: add the step that''s missing, or"
			+ " combine two steps by the same actor unless something outside the system happens"
			+ " between them.";
	static final String STARTS_WITH_SYSTEM_MSG = "it starts with the system step \"{0}\"";
	static final String TWO_SYSTEM_STEPS_MSG = "\"{0}\" and \"{1}\" are both system steps, with no"
			+ " actor step between them";
	static final String SAME_ACTOR_MSG = "\"{0}\" and \"{1}\" are both by {2}, with nothing from the"
			+ " system between them";
	/** #472: a position that merges a pair into one step. */
	static final String MERGE_MSG = "Merge into \"{0}\".";
	static final String MERGE_KIND = "MERGE_STEPS";

	private final Function<String, StepReading> reader;

	@Autowired
	public StepSequenceAssistant(NLPProcessorFactory nlpProcessorFactory) {
		this(name -> StepReading.read(nlpProcessorFactory, name));
	}

	/** For tests: reads each name with {@code reader} instead of the NLP pipeline. */
	StepSequenceAssistant(Function<String, StepReading> reader) {
		this.reader = Objects.requireNonNull(reader, "reader");
	}

	@Override
	public String assistantId() {
		return ASSISTANT_ID;
	}

	@Override
	public Class<Scenario> targetType() {
		return Scenario.class;
	}

	/** The ordinary post-edit run, and {@link #TASK_TYPE}. */
	@Override
	public boolean handlesTask(String taskType) {
		return taskType == null || TASK_TYPE.equals(taskType);
	}

	@Override
	public CleanupPolicy cleanupPolicy() {
		return CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED;
	}

	@Override
	public boolean projectSwitchable() {
		return true;
	}

	@Override
	public String displayName() {
		return "Actor and system turns";
	}

	@Override
	public AssistantResult analyze(AssistantContext context, Scenario scenario) {
		EntityRef targetRef = EntityRef.of("Scenario", scenario.getId());
		AssistantResult.Builder builder = AssistantResult.builder()
				.assistantId(ASSISTANT_ID)
				.runId(context.runId())
				.summary("Step-sequence analysis");
		try {
			List<String> breaks = new ArrayList<>();
			List<String> names = new ArrayList<>();
			List<Merge> merges = new ArrayList<>();
			findBreaks(scenario, breaks, names, merges);
			if (!breaks.isEmpty()) {
				emitIssue(builder, targetRef, scenario.getName(), breaks, names, merges);
			}
		} catch (RuntimeException e) {
			log.warn("step-sequence analysis of {} failed; skipping: {}", targetRef, e.toString());
		}
		return builder.build();
	}

	/** A step's turn: the system, or an actor by name. An unknown step has no turn (null). */
	private record Turn(boolean system, String actor) {

		static final Turn SYSTEM = new Turn(true, null);

		boolean sameActorAs(Turn other) {
			return !system && !other.system && actor.equalsIgnoreCase(other.actor);
		}
	}

	/** #472: two steps in a row that can be merged into one named {@code merged}. */
	private record Merge(Step first, Step second, String merged) {
	}

	private void findBreaks(Scenario scenario, List<String> breaks, List<String> names,
			List<Merge> merges) {
		List<Step> steps = scenario.getSteps();
		if (steps == null || steps.isEmpty()) {
			return;
		}
		PrimaryActor primary = new PrimaryActor(scenario);
		Step previousStep = null;
		StepReading previousReading = null;
		Turn previous = null;
		for (int index = 0; index < steps.size(); index++) {
			Step step = steps.get(index);
			String name = step.getName();
			StepReading reading = readingOf(step);
			Turn turn = reading == null ? null : turnOf(step, reading, primary);
			if (index == 0) {
				if (turn != null && turn.system()) {
					breaks.add(MessageFormat.format(STARTS_WITH_SYSTEM_MSG, name));
					names.add(name);
				}
			} else if (previous != null && turn != null) {
				String previousName = previousStep.getName();
				boolean pair = false;
				if (previous.system() && turn.system()) {
					breaks.add(MessageFormat.format(TWO_SYSTEM_STEPS_MSG, previousName, name));
					pair = true;
				} else if (previous.sameActorAs(turn)) {
					breaks.add(MessageFormat.format(SAME_ACTOR_MSG, previousName, name,
							previous.actor()));
					pair = true;
				}
				if (pair) {
					names.add(previousName);
					names.add(name);
					Merge merge = mergeOf(scenario, previousStep, previousReading, step, reading);
					if (merge != null) {
						merges.add(merge);
					}
				}
			}
			previousStep = step;
			previousReading = reading;
			previous = turn;
		}
	}

	/**
	 * #472: the merge of two steps, or null when there is none to offer: the first must read
	 * "&lt;subject&gt; ..." and the second must have a predicate ({@link StepVerbs#predicate});
	 * neither may be a scenario used as a step; the first may not be used by another scenario
	 * (renaming it would rename it there); and the merged name must fit a name.
	 */
	private static Merge mergeOf(Scenario scenario, Step first, StepReading firstReading,
			Step second, StepReading secondReading) {
		if (isScenario(first) || isScenario(second)
				|| firstReading.kind() != StepReading.Kind.SUBJECT) {
			return null;
		}
		if (first.getUsingScenarios() != null && first.getUsingScenarios().stream()
				.anyMatch(other -> !other.equals(scenario))) {
			return null;
		}
		String rest = StepVerbs.predicate(second.getName(), secondReading);
		if (rest == null) {
			return null;
		}
		String head = first.getName().trim();
		while (head.endsWith(".")) {
			head = head.substring(0, head.length() - 1).trim();
		}
		String merged = head + " and " + rest;
		return merged.length() > StepStructureAssistant.MAX_NAME_LENGTH ? null
				: new Merge(first, second, merged);
	}

	/** A scenario used as a step; asked of the entity, since a step may be a lazy proxy. */
	private static boolean isScenario(Step step) {
		return step instanceof Scenario
				|| Scenario.class.equals(step.getProjectOrDomainEntityInterface());
	}

	private StepReading readingOf(Step step) {
		String name = step.getName();
		// #486: the parser's reading, or the step's own words when it can't read them.
		return name == null || name.isBlank() ? null
				: StepReading.withKnownSubjects(reader.apply(name), name,
						StepStructureAssistant.actorNames(step));
	}

	private Turn turnOf(Step step, StepReading reading, PrimaryActor primary) {
		switch (reading.kind()) {
		case COMMAND:
			Actor actor = primary.get();
			return actor == null || actor.getName() == null ? null : new Turn(false, actor.getName());
		case SUBJECT:
			return subjectTurn(step.getProjectOrDomain(), reading.subject());
		default:
			return null;
		}
	}

	private static Turn subjectTurn(ProjectOrDomain projectOrDomain, String subject) {
		if (StepStructureAssistant.isSystem(subject)) {
			return Turn.SYSTEM;
		}
		if (projectOrDomain != null) {
			if (StepStructureAssistant.projectNameIn(subject, projectOrDomain.getName()) != null) {
				return null;
			}
			for (Actor actor : projectOrDomain.getActors()) {
				if (StepStructureAssistant.namesActor(subject, actor.getName())) {
					return new Turn(false, actor.getName());
				}
			}
		}
		String bare = StepStructureAssistant.withoutArticle(subject);
		return bare.isEmpty() ? null : new Turn(false, bare);
	}

	/**
	 * The single primary actor of the use cases the scenario is in, looked up once, and only if a
	 * command step needs it. Null when there is no use case, or their primary actors differ.
	 */
	private static final class PrimaryActor {

		private final Scenario scenario;
		private boolean resolved;
		private Actor actor;

		PrimaryActor(Scenario scenario) {
			this.scenario = scenario;
		}

		Actor get() {
			if (!resolved) {
				resolved = true;
				List<Actor> primaries = StepStructureAssistant.useCasesOfScenario(scenario).stream()
						.map(UseCase::getPrimaryActor).filter(Objects::nonNull).distinct().toList();
				actor = primaries.size() == 1 ? primaries.get(0) : null;
			}
			return actor;
		}
	}

	/**
	 * #472: the issue is keyed by the breaks it lists, not the scenario alone. An unchanged
	 * scenario keeps its issue; a different set of breaks is a new issue. Otherwise, once a
	 * person resolved the issue (by accepting one merge, say), the breaks left over would be
	 * written onto the resolved issue and never show.
	 */
	static String digest(List<String> breaks) {
		try {
			byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
					.digest(String.join("\n", breaks).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(hash, 0, 6);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is always available", e);
		}
	}

	private void emitIssue(AssistantResult.Builder builder, EntityRef targetRef, String scenarioName,
			List<String> breaks, List<String> names, List<Merge> merges) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":sequence:" + digest(breaks);
		List<EvidenceRef> evidence = new ArrayList<>();
		evidence.add(EvidenceRef.ofLocator("property=steps"));
		names.stream().distinct().map(EvidenceRef::ofSnippet).forEach(evidence::add);
		String text = MessageFormat.format(ISSUE_MSG, scenarioName, String.join("; ", breaks));
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType", FINDING_TYPE)));
		for (Merge merge : merges) {
			builder.annotationAction(new AnnotationAction(
					key + ":merge:" + merge.first().getId() + ":" + merge.second().getId(),
					AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key,
					MessageFormat.format(MERGE_MSG, merge.merged()), null, null,
					List.of(EvidenceRef.ofSnippet(merge.first().getName()),
							EvidenceRef.ofSnippet(merge.second().getName())),
					Map.of("kind", MERGE_KIND, "proposedName", merge.merged(), "expectedName",
							merge.first().getName(), "expectedSecondName",
							merge.second().getName())));
		}
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}
}
