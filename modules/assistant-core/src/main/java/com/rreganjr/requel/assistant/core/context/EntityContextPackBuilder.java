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
package com.rreganjr.requel.assistant.core.context;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Builds an {@link EntityContextPack} for a single target entity. Resolves
 * the target into the appropriate {@link EntitySnapshot}, collects its
 * annotations and related glossary terms, and stamps redaction / truncation
 * notes into {@link ContextPackMetadata}.
 *
 * <p>Parents and children are intentionally empty in this first slice; the
 * domain {@code getReferers()} accessors return container interfaces that
 * are not always {@link ProjectOrDomainEntity}, so capturing them with
 * stable {@link EntityRef} values needs additional adapter work. They can be
 * populated in a follow-up without changing the pack contract.</p>
 */
@Component
public class EntityContextPackBuilder {

	private final RedactionPolicy redactionPolicy;
	private final ContextPackSizeLimits limits;
	private final Clock clock;

	@Autowired
	public EntityContextPackBuilder(RedactionPolicy redactionPolicy,
			ContextPackSizeLimits limits) {
		this(redactionPolicy, limits, Clock.systemUTC());
	}

	EntityContextPackBuilder(RedactionPolicy redactionPolicy, ContextPackSizeLimits limits,
			Clock clock) {
		this.redactionPolicy = Objects.requireNonNull(redactionPolicy, "redactionPolicy");
		this.limits = Objects.requireNonNull(limits, "limits");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public EntityContextPack build(Object target) {
		Objects.requireNonNull(target, "target");
		List<String> redacted = new ArrayList<>();
		List<String> truncated = new ArrayList<>();
		ContextPackBudget budget = new ContextPackBudget(limits.getMaxTotalCharacters());
		int maxField = limits.getMaxTextCharsPerField();

		EntityRef targetRef = entityRefFor(target);
		EntitySnapshot snapshot = snapshotFor(target, maxField, redacted, truncated, budget);

		List<AnnotationSnapshot> annotations = new ArrayList<>();
		if (target instanceof com.rreganjr.requel.annotation.Annotatable annotatable) {
			int maxAnnotations = limits.getMaxAnnotationsPerEntity();
			int count = 0;
			for (Annotation annotation : annotatable.getAnnotations()) {
				if (count >= maxAnnotations) {
					truncated.add("annotations list capped at " + maxAnnotations);
					break;
				}
				if (budget.exceeded()) {
					truncated.add("annotations list truncated by total-character budget");
					break;
				}
				String annText = ContextPackTextUtils.prepareText(
						"annotation[" + count + "].text", annotation.getText(), maxField,
						redactionPolicy, redacted, truncated);
				annotations.add(new AnnotationSnapshot(annotation.getId(), annotation.getVersion(),
						annotationKind(annotation), annText, annotation.isMustBeResolved(),
						annotation.isResolved(),
						ContextPackTextUtils.username(annotation.getCreatedBy()),
						toInstant(annotation.getDateCreated())));
				budget.add(annText);
				count++;
			}
		}

		List<GlossaryTermSnapshot> relatedTerms = new ArrayList<>();
		if (target instanceof ProjectOrDomainEntity entity) {
			for (GlossaryTerm term : entity.getGlossaryTerms()) {
				if (budget.exceeded()) {
					truncated.add("relatedTerms list truncated by total-character budget");
					break;
				}
				String text = ContextPackTextUtils.prepareText(
						"relatedTerm[" + term.getId() + "].text", term.getText(), maxField,
						redactionPolicy, redacted, truncated);
				relatedTerms.add(new GlossaryTermSnapshot(term.getId(), term.getVersion(),
						term.getName(), text));
				budget.add(term.getName(), text);
			}
		}

		ContextPackMetadata metadata = new ContextPackMetadata(Instant.now(clock),
				budget.totalCharacters(), !truncated.isEmpty(), redacted, truncated);
		return new EntityContextPack(targetRef, snapshot, List.of(), List.of(), annotations,
				relatedTerms, metadata);
	}

	private EntityRef entityRefFor(Object target) {
		if (target instanceof Project project) {
			return EntityRef.of("Project", project.getId());
		}
		if (target instanceof Scenario scenario) {
			return EntityRef.of("Scenario", scenario.getId());
		}
		if (target instanceof Step step) {
			return EntityRef.of("Step", step.getId());
		}
		if (target instanceof Goal goal) {
			return EntityRef.of("Goal", goal.getId());
		}
		if (target instanceof Story story) {
			return EntityRef.of("Story", story.getId());
		}
		if (target instanceof Actor actor) {
			return EntityRef.of("Actor", actor.getId());
		}
		if (target instanceof UseCase useCase) {
			return EntityRef.of("UseCase", useCase.getId());
		}
		if (target instanceof GlossaryTerm term) {
			return EntityRef.of("GlossaryTerm", term.getId());
		}
		throw new IllegalArgumentException(
				"Unsupported target type for EntityContextPack: " + target.getClass().getName());
	}

	private EntitySnapshot snapshotFor(Object target, int maxField, List<String> redacted,
			List<String> truncated, ContextPackBudget budget) {
		if (target instanceof Project project) {
			String text = ContextPackTextUtils.prepareText("project.text", project.getText(),
					maxField, redactionPolicy, redacted, truncated);
			budget.add(project.getName(), text);
			return new ProjectSnapshot(project.getId(), project.getVersion(), project.getName(),
					text, ContextPackTextUtils.username(project.getCreatedBy()));
		}
		if (target instanceof Scenario scenario) {
			String text = ContextPackTextUtils.prepareText("scenario.text", scenario.getText(),
					maxField, redactionPolicy, redacted, truncated);
			String typeName = scenario.getType() != null ? scenario.getType().name() : null;
			List<StepSnapshot> steps = new ArrayList<>();
			for (Step step : scenario.getSteps()) {
				String stepText = ContextPackTextUtils.prepareText("scenario.step[" + step.getId()
						+ "].text", step.getText(), maxField, redactionPolicy, redacted, truncated);
				steps.add(new StepSnapshot(step.getId(), step.getVersion(), step.getName(), stepText,
						step instanceof Scenario));
				budget.add(step.getName(), stepText);
			}
			budget.add(scenario.getName(), text);
			return new ScenarioSnapshot(scenario.getId(), scenario.getVersion(), scenario.getName(),
					text, typeName, steps);
		}
		if (target instanceof Step step) {
			String text = ContextPackTextUtils.prepareText("step.text", step.getText(), maxField,
					redactionPolicy, redacted, truncated);
			budget.add(step.getName(), text);
			return new StepSnapshot(step.getId(), step.getVersion(), step.getName(), text, false);
		}
		if (target instanceof Goal goal) {
			String text = ContextPackTextUtils.prepareText("goal.text", goal.getText(), maxField,
					redactionPolicy, redacted, truncated);
			budget.add(goal.getName(), text);
			return new GoalSnapshot(goal.getId(), goal.getVersion(), goal.getName(), text);
		}
		if (target instanceof Story story) {
			String text = ContextPackTextUtils.prepareText("story.text", story.getText(), maxField,
					redactionPolicy, redacted, truncated);
			EntityRef primaryActor = story.getPrimaryActor() != null
					? EntityRef.of("Actor", story.getPrimaryActor().getId())
					: null;
			String storyTypeName = story.getStoryType() != null ? story.getStoryType().name() : null;
			budget.add(story.getName(), text);
			return new StorySnapshot(story.getId(), story.getVersion(), story.getName(), text,
					storyTypeName, primaryActor);
		}
		if (target instanceof Actor actor) {
			String text = ContextPackTextUtils.prepareText("actor.text", actor.getText(), maxField,
					redactionPolicy, redacted, truncated);
			budget.add(actor.getName(), text);
			return new ActorSnapshot(actor.getId(), actor.getVersion(), actor.getName(), text);
		}
		if (target instanceof UseCase useCase) {
			String text = ContextPackTextUtils.prepareText("useCase.text", useCase.getText(),
					maxField, redactionPolicy, redacted, truncated);
			EntityRef primaryActor = useCase.getPrimaryActor() != null
					? EntityRef.of("Actor", useCase.getPrimaryActor().getId())
					: null;
			EntityRef primaryScenario = useCase.getScenario() != null
					? EntityRef.of("Scenario", useCase.getScenario().getId())
					: null;
			budget.add(useCase.getName(), text);
			return new UseCaseSnapshot(useCase.getId(), useCase.getVersion(), useCase.getName(),
					text, primaryActor, primaryScenario);
		}
		if (target instanceof GlossaryTerm term) {
			String text = ContextPackTextUtils.prepareText("glossaryTerm.text", term.getText(),
					maxField, redactionPolicy, redacted, truncated);
			budget.add(term.getName(), text);
			return new GlossaryTermSnapshot(term.getId(), term.getVersion(), term.getName(), text);
		}
		throw new IllegalArgumentException(
				"Unsupported target type for EntityContextPack: " + target.getClass().getName());
	}

	/**
	 * Map an {@link Annotation} subtype to its pack-level {@link AnnotationKind}.
	 * Only {@code Issue} and {@code Note} are reachable here; {@code Argument}
	 * and {@code Position} are separate type hierarchies that do not extend
	 * {@link Annotation} (they hang off {@link Issue#getPositions()} and
	 * {@link com.rreganjr.requel.annotation.Position#getArguments()}
	 * respectively). The {@code ARGUMENT} value on {@link AnnotationKind}
	 * exists for {@code AnnotationAction.createArgument(...)} on the write
	 * path, not for pack-level reads.
	 */
	private static AnnotationKind annotationKind(Annotation annotation) {
		if (annotation instanceof Issue) {
			return AnnotationKind.ISSUE;
		}
		if (annotation instanceof Note) {
			return AnnotationKind.NOTE;
		}
		return AnnotationKind.NOTE;
	}

	private static Instant toInstant(Date date) {
		return date == null ? null : date.toInstant();
	}
}
