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
import com.rreganjr.requel.assistant.core.freshness.MachineAnnotations;
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
	/** Issue #261: null in unit tests that build the base pack only. */
	private ContextProviderRegistry providers;

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

	/** Issue #261: the context providers; optional, so the base pack builds without them. */
	@Autowired(required = false)
	public void setProviders(ContextProviderRegistry providers) {
		this.providers = providers;
	}

	/** The base pack only: {@code build(target, PackSpec.entityOnly())}. */
	public EntityContextPack build(Object target) {
		return build(target, PackSpec.entityOnly());
	}

	/**
	 * The base pack plus a section from each provider {@code spec} names, in its order (#261).
	 * Each provider gets its share of what the base pack left of the cap; one that would get
	 * nothing is skipped and noted. The base pack itself is built exactly as before.
	 *
	 * @throws IllegalArgumentException if {@code spec} names a provider that is not registered
	 */
	public EntityContextPack build(Object target, PackSpec spec) {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(spec, "spec");
		// #262: the project's switches decide which categories are masked; authors become roles
		RedactionPolicy policy = redactionPolicy
				.forProject(ContextPackTextUtils.projectIdOf(target));
		AuthorPseudonyms authors = new AuthorPseudonyms();
		List<String> redacted = new ArrayList<>();
		List<String> truncated = new ArrayList<>();
		int cap = spec.maxCharacters() > 0
				? Math.min(limits.getMaxTotalCharacters(), spec.maxCharacters())
				: limits.getMaxTotalCharacters();
		ContextPackBudget budget = new ContextPackBudget(cap);
		int maxField = limits.getMaxTextCharsPerField();

		EntityRef targetRef = entityRefFor(target);
		EntitySnapshot snapshot = snapshotFor(target, maxField, redacted, truncated, budget,
				policy, authors);

		List<AnnotationSnapshot> annotations = new ArrayList<>();
		if (target instanceof com.rreganjr.requel.annotation.Annotatable annotatable) {
			int maxAnnotations = limits.getMaxAnnotationsPerEntity();
			int count = 0;
			for (Annotation annotation : annotatable.getAnnotations()) {
				// Keep machine-generated annotations out of the context: assistants (especially
				// smaller models) tend to echo existing automated findings back as their own
				// instead of analyzing the requirement. Human annotations remain as useful context.
				if (isMachineGenerated(annotation)) {
					continue;
				}
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
						policy, redacted, truncated);
				annotations.add(new AnnotationSnapshot(annotation.getId(), annotation.getVersion(),
						annotationKind(annotation), annText, annotation.isMustBeResolved(),
						annotation.isResolved(),
						authors.of(annotation.getCreatedBy()),
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
						policy, redacted, truncated);
				String termName = ContextPackTextUtils.prepareName(
						"relatedTerm[" + term.getId() + "].name", term.getName(), policy, redacted);
				relatedTerms.add(new GlossaryTermSnapshot(term.getId(), term.getVersion(),
						termName, text));
				budget.add(termName, text);
			}
		}

		List<ContextSection> sections = contribute(target, spec, cap, budget, policy, authors,
				redacted, truncated, maxField);

		ContextPackMetadata metadata = new ContextPackMetadata(Instant.now(clock),
				budget.totalCharacters(), !truncated.isEmpty(), redacted, truncated);
		return new EntityContextPack(targetRef, snapshot, List.of(), List.of(), annotations,
				relatedTerms, sections, metadata);
	}

	private List<ContextSection> contribute(Object target, PackSpec spec, int cap,
			ContextPackBudget budget, RedactionPolicy policy, AuthorPseudonyms authors,
			List<String> redacted, List<String> truncated, int maxField) {
		List<ContextSection> sections = new ArrayList<>();
		for (String id : spec.providerIds()) {
			if (ContextProviderRegistry.ENTITY.equals(id)) {
				continue;
			}
			ContextProvider provider = providers == null ? null : providers.find(id);
			if (provider == null) {
				throw new IllegalArgumentException("unknown context provider " + id);
			}
			if (!provider.appliesTo(target)) {
				continue;
			}
			int share = Math.min(spec.budgets().getOrDefault(id, limits.getProviderBudget()),
					cap - budget.totalCharacters());
			if (share <= 0) {
				truncated.add(id + ": skipped, the pack's character budget is used up");
				continue;
			}
			ProviderBudget providerBudget = new ProviderBudget(share);
			ContextSection section = provider.contribute(target, providerBudget,
					new ProviderContext(id, policy, authors, redacted, truncated, maxField));
			if (section == null || section.entities().isEmpty() && section.available() == 0) {
				continue;
			}
			if (section.truncated()) {
				truncated.add(id + ": " + section.shown() + " of " + section.available()
						+ " shown");
			}
			budget.addCharacters(providerBudget.used());
			sections.add(section);
		}
		return sections;
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
			List<String> truncated, ContextPackBudget budget, RedactionPolicy policy,
			AuthorPseudonyms authors) {
		if (target instanceof Project project) {
			String name = ContextPackTextUtils.prepareName("project.name", project.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("project.text", project.getText(),
					maxField, policy, redacted, truncated);
			budget.add(name, text);
			return new ProjectSnapshot(project.getId(), project.getVersion(), name,
					text, authors.of(project.getCreatedBy()));
		}
		if (target instanceof Scenario scenario) {
			String name = ContextPackTextUtils.prepareName("scenario.name", scenario.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("scenario.text", scenario.getText(),
					maxField, policy, redacted, truncated);
			String typeName = scenario.getType() != null ? scenario.getType().name() : null;
			List<StepSnapshot> steps = new ArrayList<>();
			for (Step step : scenario.getSteps()) {
				String stepText = ContextPackTextUtils.prepareText("scenario.step[" + step.getId()
						+ "].text", step.getText(), maxField, policy, redacted, truncated);
				String stepName = ContextPackTextUtils.prepareName("scenario.step[" + step.getId()
						+ "].name", step.getName(), policy, redacted);
				steps.add(new StepSnapshot(step.getId(), step.getVersion(), stepName, stepText,
						step instanceof Scenario));
				budget.add(stepName, stepText);
			}
			budget.add(name, text);
			return new ScenarioSnapshot(scenario.getId(), scenario.getVersion(), name,
					text, typeName, steps);
		}
		if (target instanceof Step step) {
			String name = ContextPackTextUtils.prepareName("step.name", step.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("step.text", step.getText(), maxField,
					policy, redacted, truncated);
			budget.add(name, text);
			return new StepSnapshot(step.getId(), step.getVersion(), name, text, false);
		}
		if (target instanceof Goal goal) {
			String name = ContextPackTextUtils.prepareName("goal.name", goal.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("goal.text", goal.getText(), maxField,
					policy, redacted, truncated);
			budget.add(name, text);
			return new GoalSnapshot(goal.getId(), goal.getVersion(), name, text);
		}
		if (target instanceof Story story) {
			String name = ContextPackTextUtils.prepareName("story.name", story.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("story.text", story.getText(), maxField,
					policy, redacted, truncated);
			EntityRef primaryActor = story.getPrimaryActor() != null
					? EntityRef.of("Actor", story.getPrimaryActor().getId())
					: null;
			String storyTypeName = story.getStoryType() != null ? story.getStoryType().name() : null;
			budget.add(name, text);
			return new StorySnapshot(story.getId(), story.getVersion(), name, text,
					storyTypeName, primaryActor);
		}
		if (target instanceof Actor actor) {
			String name = ContextPackTextUtils.prepareName("actor.name", actor.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("actor.text", actor.getText(), maxField,
					policy, redacted, truncated);
			budget.add(name, text);
			return new ActorSnapshot(actor.getId(), actor.getVersion(), name, text);
		}
		if (target instanceof UseCase useCase) {
			String name = ContextPackTextUtils.prepareName("useCase.name", useCase.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("useCase.text", useCase.getText(),
					maxField, policy, redacted, truncated);
			EntityRef primaryActor = useCase.getPrimaryActor() != null
					? EntityRef.of("Actor", useCase.getPrimaryActor().getId())
					: null;
			EntityRef primaryScenario = useCase.getScenario() != null
					? EntityRef.of("Scenario", useCase.getScenario().getId())
					: null;
			budget.add(name, text);
			return new UseCaseSnapshot(useCase.getId(), useCase.getVersion(), name,
					text, primaryActor, primaryScenario);
		}
		if (target instanceof GlossaryTerm term) {
			String name = ContextPackTextUtils.prepareName("glossaryTerm.name", term.getName(), policy,
					redacted);
			String text = ContextPackTextUtils.prepareText("glossaryTerm.text", term.getText(),
					maxField, policy, redacted, truncated);
			budget.add(name, text);
			return new GlossaryTermSnapshot(term.getId(), term.getVersion(), name, text);
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
	/**
	 * True if an assistant wrote the annotation, so it is kept out of context packs: an
	 * {@code "ASSISTANT:<id>"} source, or (issue #270) no source and created by the assistant user,
	 * which is what the old lexical path left. Human annotations are kept.
	 */
	private static boolean isMachineGenerated(Annotation annotation) {
		return MachineAnnotations.isMachineGenerated(annotation);
	}

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
