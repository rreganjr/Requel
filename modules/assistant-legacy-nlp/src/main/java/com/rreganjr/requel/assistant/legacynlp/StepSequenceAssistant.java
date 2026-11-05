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

	private static final String DO_NOTHING_MSG = "Do nothing.";
	static final String ISSUE_MSG = "The steps of \"{0}\" don''t alternate between an actor and the"
			+ " system: {1}. An actor acts and the system responds: add the step that''s missing, or"
			+ " combine two steps by the same actor unless something outside the system happens"
			+ " between them.";
	static final String STARTS_WITH_SYSTEM_MSG = "it starts with the system step \"{0}\"";
	static final String TWO_SYSTEM_STEPS_MSG = "\"{0}\" and \"{1}\" are both system steps, with no"
			+ " actor step between them";
	static final String SAME_ACTOR_MSG = "\"{0}\" and \"{1}\" are both by {2}, with nothing from the"
			+ " system between them";

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
			findBreaks(scenario, breaks, names);
			if (!breaks.isEmpty()) {
				emitIssue(builder, targetRef, scenario.getName(), breaks, names);
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

	private void findBreaks(Scenario scenario, List<String> breaks, List<String> names) {
		List<Step> steps = scenario.getSteps();
		if (steps == null || steps.isEmpty()) {
			return;
		}
		PrimaryActor primary = new PrimaryActor(scenario);
		Turn previous = null;
		String previousName = null;
		for (int index = 0; index < steps.size(); index++) {
			Step step = steps.get(index);
			String name = step.getName();
			Turn turn = turnOf(step, primary);
			if (index == 0) {
				if (turn != null && turn.system()) {
					breaks.add(MessageFormat.format(STARTS_WITH_SYSTEM_MSG, name));
					names.add(name);
				}
			} else if (previous != null && turn != null) {
				if (previous.system() && turn.system()) {
					breaks.add(MessageFormat.format(TWO_SYSTEM_STEPS_MSG, previousName, name));
					names.add(previousName);
					names.add(name);
				} else if (previous.sameActorAs(turn)) {
					breaks.add(MessageFormat.format(SAME_ACTOR_MSG, previousName, name,
							previous.actor()));
					names.add(previousName);
					names.add(name);
				}
			}
			previous = turn;
			previousName = name;
		}
	}

	private Turn turnOf(Step step, PrimaryActor primary) {
		String name = step.getName();
		if (name == null || name.isBlank()) {
			return null;
		}
		StepReading reading = reader.apply(name);
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

	private void emitIssue(AssistantResult.Builder builder, EntityRef targetRef, String scenarioName,
			List<String> breaks, List<String> names) {
		String key = ASSISTANT_ID + ":" + targetRef.entityType() + ":" + targetRef.entityId()
				+ ":sequence";
		List<EvidenceRef> evidence = new ArrayList<>();
		evidence.add(EvidenceRef.ofLocator("property=steps"));
		names.stream().distinct().map(EvidenceRef::ofSnippet).forEach(evidence::add);
		String text = MessageFormat.format(ISSUE_MSG, scenarioName, String.join("; ", breaks));
		builder.annotationAction(new AnnotationAction(key,
				AnnotationAction.ActionType.CREATE_OR_UPDATE_ISSUE, targetRef, null, text, "LOW",
				null, evidence, Map.of("mustResolve", Boolean.FALSE, "findingType", FINDING_TYPE)));
		builder.annotationAction(new AnnotationAction(key + ":do-nothing",
				AnnotationAction.ActionType.CREATE_OR_UPDATE_POSITION, null, key, DO_NOTHING_MSG,
				null, null, evidence, Map.of()));
	}
}
