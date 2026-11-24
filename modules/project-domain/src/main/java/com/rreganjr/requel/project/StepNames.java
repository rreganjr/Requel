/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * #468: how a step's (or a scenario's) name reads once its actor role is filled in, and which use
 * cases a step is in.
 * <p>
 * A step with no role reads as its name. A step with a role reads as the actor followed by the
 * action it stores: the primary actor of the use case it is shown in, "The system", or the actor it
 * names. Away from one use case the primary actor is filled in only when every use case the step
 * reaches has the same one; otherwise, and in no use case at all, the role reads "Primary actor".
 */
public final class StepNames {

	/** What a primary-actor role reads as when no single primary actor applies. */
	public static final String PRIMARY_ACTOR_LABEL = "Primary actor";
	/** What a system role reads as. */
	public static final String SYSTEM_LABEL = "The system";
	/** What a named role reads as if its actor is missing (it is cleared on actor delete). */
	public static final String UNNAMED_ACTOR_LABEL = "An actor";

	private StepNames() {
	}

	/**
	 * The step's name as it reads in a use case whose primary actor is the one given; a null
	 * primary actor reads as the role.
	 */
	public static String render(Step step, Actor primaryActor) {
		if (step == null) {
			return null;
		}
		String actor = actorLabel(step.getActorRole(), step.getActor(), primaryActor);
		return actor == null ? step.getName() : join(actor, step.getName());
	}

	/**
	 * The step's name as it reads across the use cases given (rule 7 of the #468 plan): with the
	 * primary actor when they all have the same one, otherwise with the role.
	 */
	public static String renderFor(Step step, Collection<UseCase> useCases) {
		return render(step, singlePrimaryActor(useCases));
	}

	/** The step's name as it reads across every use case it is in. */
	public static String display(Step step) {
		if (step == null) {
			return null;
		}
		return step.getActorRole() == ActorRole.PRIMARY_ACTOR ? renderFor(step, useCasesOf(step))
				: render(step, null);
	}

	/**
	 * What fills the actor's place, or null for a step with no role.
	 */
	public static String actorLabel(ActorRole role, Actor named, Actor primaryActor) {
		if (role == null) {
			return null;
		}
		switch (role) {
		case PRIMARY_ACTOR:
			return primaryActor == null || primaryActor.getName() == null ? PRIMARY_ACTOR_LABEL
					: primaryActor.getName();
		case SYSTEM:
			return SYSTEM_LABEL;
		case NAMED:
		default:
			return named == null || named.getName() == null ? UNNAMED_ACTOR_LABEL : named.getName();
		}
	}

	/**
	 * The one primary actor every use case given has, or null when there are none, they differ,
	 * or one has none.
	 */
	public static Actor singlePrimaryActor(Collection<UseCase> useCases) {
		if (useCases == null || useCases.isEmpty()) {
			return null;
		}
		Actor found = null;
		for (UseCase useCase : useCases) {
			Actor primary = useCase.getPrimaryActor();
			if (primary == null || (found != null && !Objects.equals(found, primary))) {
				return null;
			}
			found = primary;
		}
		return found;
	}

	/**
	 * The use cases a step is in, by name: those whose primary or additional scenarios contain
	 * it, directly or through scenarios used as steps. For a scenario this includes the use cases
	 * it is itself a scenario of. (#459/#455, moved here from StepStructureAssistant by #468.)
	 */
	public static List<UseCase> useCasesOf(Step step) {
		if (step instanceof Scenario scenario) {
			return useCasesOfScenario(scenario);
		}
		return useCasesContaining(step);
	}

	/**
	 * The use cases whose scenarios contain the step as a step, directly or through scenarios used
	 * as steps; for a scenario, not the use cases it is itself a scenario of.
	 */
	public static List<UseCase> useCasesContaining(Step step) {
		return useCasesReachedFrom(step.getUsingScenarios(), step);
	}

	/**
	 * The use cases a scenario is in, by name: those whose primary or additional scenarios are the
	 * scenario itself or contain it through scenarios used as steps.
	 */
	public static List<UseCase> useCasesOfScenario(Scenario scenario) {
		return useCasesReachedFrom(List.of(scenario), scenario);
	}

	private static List<UseCase> useCasesReachedFrom(Collection<Scenario> start, Step step) {
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

	private static String join(String actor, String action) {
		if (action == null || action.isBlank()) {
			return actor;
		}
		return actor + " " + action.strip();
	}
}
