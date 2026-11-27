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
package com.rreganjr.requel.service.query;

import static com.rreganjr.requel.service.api.dto.TraceGapDto.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalTraceDto;
import com.rreganjr.requel.service.api.dto.TraceGapDto;
import com.rreganjr.requel.service.api.dto.TraceabilityDto;

/**
 * #497: the goal matrix and the gaps for a project, from the links the model already has. Every
 * entity is visited once; nothing is written.
 */
@Service
@Transactional(readOnly = true)
public class TraceabilityService {

	private static final Comparator<EntityReferenceDto> BY_NAME = Comparator
			.comparing((EntityReferenceDto r) -> r.name() == null ? "" : r.name(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(EntityReferenceDto::id);

	public TraceabilityDto traceability(Project project) {
		List<GoalTraceDto> goals = new ArrayList<>();
		Map<String, List<EntityReferenceDto>> gaps = new HashMap<>();
		KINDS.forEach(kind -> gaps.put(kind, new ArrayList<>()));

		for (Goal goal : byId(project.getGoals(), Goal::getId)) {
			List<EntityReferenceDto> stakeholders = new ArrayList<>();
			List<EntityReferenceDto> actors = new ArrayList<>();
			List<EntityReferenceDto> stories = new ArrayList<>();
			List<EntityReferenceDto> useCases = new ArrayList<>();
			for (GoalContainer referer : goal.getReferers()) {
				if (referer instanceof Stakeholder s) {
					stakeholders.add(ref("Stakeholder", s.getId(), s.getDisplayName()));
				} else if (referer instanceof Actor a) {
					actors.add(ref("Actor", a.getId(), a.getName()));
				} else if (referer instanceof Story s) {
					stories.add(ref("Story", s.getId(), s.getName()));
				} else if (referer instanceof UseCase u) {
					useCases.add(ref("UseCase", u.getId(), u.getName()));
				}
			}
			stakeholders.sort(BY_NAME);
			actors.sort(BY_NAME);
			stories.sort(BY_NAME);
			useCases.sort(BY_NAME);
			boolean realized = !stories.isEmpty() || !useCases.isEmpty();
			goals.add(new GoalTraceDto(goal.getId(), goal.getName(), realized, stakeholders, actors,
					stories, useCases));
			if (!realized) {
				gaps.get(GOAL_NOT_REALIZED).add(ref("Goal", goal.getId(), goal.getName()));
			}
		}

		Set<Actor> usedActors = usedActors(project);
		for (Actor actor : byId(project.getActors(), Actor::getId)) {
			if (!usedActors.contains(actor)) {
				gaps.get(ACTOR_NOT_USED).add(ref("Actor", actor.getId(), actor.getName()));
			}
		}

		for (Story story : byId(project.getStories(), Story::getId)) {
			if (story.getGoals().isEmpty()) {
				gaps.get(STORY_WITHOUT_GOAL).add(ref("Story", story.getId(), story.getName()));
			}
			if (story.getReferers().stream().noneMatch(UseCase.class::isInstance)) {
				gaps.get(STORY_NOT_IN_USE_CASE).add(ref("Story", story.getId(), story.getName()));
			}
		}

		for (UseCase useCase : byId(project.getUseCases(), UseCase::getId)) {
			if (useCase.getGoals().isEmpty()) {
				gaps.get(USE_CASE_WITHOUT_GOAL).add(ref("UseCase", useCase.getId(), useCase.getName()));
			}
		}

		Set<Scenario> reached = scenariosReachedByUseCases(project);
		for (Scenario scenario : byId(project.getScenarios(), Scenario::getId)) {
			if (!reached.contains(scenario)) {
				gaps.get(SCENARIO_NOT_IN_USE_CASE).add(ref("Scenario", scenario.getId(), scenario.getName()));
			}
		}

		for (Stakeholder stakeholder : byId(project.getStakeholders(), Stakeholder::getId)) {
			if (stakeholder.getGoals().isEmpty()) {
				gaps.get(STAKEHOLDER_WITHOUT_GOAL).add(
						ref("Stakeholder", stakeholder.getId(), stakeholder.getDisplayName()));
			}
		}

		List<TraceGapDto> flat = new ArrayList<>();
		for (String kind : KINDS) {
			gaps.get(kind).forEach(entity -> flat.add(new TraceGapDto(kind, entity)));
		}
		return new TraceabilityDto(goals, flat);
	}

	/**
	 * Actors a story or use case lists, a use case has as primary actor, or a step or scenario
	 * names as who does it (#468).
	 */
	private static Set<Actor> usedActors(Project project) {
		Set<Actor> used = new HashSet<>();
		for (Story story : project.getStories()) {
			used.addAll(story.getActors());
		}
		for (UseCase useCase : project.getUseCases()) {
			used.addAll(useCase.getActors());
			if (useCase.getPrimaryActor() != null) {
				used.add(useCase.getPrimaryActor());
			}
		}
		for (Scenario scenario : project.getScenarios()) {
			if (scenario.getActor() != null) {
				used.add(scenario.getActor());
			}
			for (Step step : scenario.getSteps()) {
				if (step.getActor() != null) {
					used.add(step.getActor());
				}
			}
		}
		return used;
	}

	/**
	 * The scenarios a use case reaches: its main and additional scenarios, and every scenario
	 * those use as a step, at any depth. The visited set stops a cycle.
	 */
	private static Set<Scenario> scenariosReachedByUseCases(Project project) {
		Set<Scenario> reached = new HashSet<>();
		List<Scenario> todo = new ArrayList<>();
		for (UseCase useCase : project.getUseCases()) {
			if (useCase.getScenario() != null) {
				todo.add(useCase.getScenario());
			}
			todo.addAll(useCase.getAdditionalScenarios());
		}
		while (!todo.isEmpty()) {
			Scenario scenario = todo.remove(todo.size() - 1);
			if (reached.add(scenario)) {
				for (Step step : scenario.getSteps()) {
					if (step instanceof Scenario sub) {
						todo.add(sub);
					}
				}
			}
		}
		return reached;
	}

	private static <T> List<T> byId(Set<? extends T> entities, Function<T, Long> id) {
		List<T> sorted = new ArrayList<>(entities);
		sorted.sort(Comparator.comparing(id, Comparator.nullsLast(Comparator.naturalOrder())));
		return sorted;
	}

	private static EntityReferenceDto ref(String type, Long id, String name) {
		return new EntityReferenceDto(type, id, name);
	}
}
