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
package com.rreganjr.requel.assistant.core.context.provider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;

/**
 * Issue #261: a goal's relations (#257's seven types) with the related goals' name and text. The
 * relation reads from the reviewed goal: an outgoing relation by its label ({@code Refines}), an
 * incoming one by its inverse ({@code Refined by}). A symmetric relation recorded both ways
 * appears once.
 */
@Component
public class GoalRelationsProvider extends AbstractContextProvider {

	public static final String ID = "goal-relations";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof Goal;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		Goal goal = (Goal) target;
		List<Edge> edges = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (GoalRelation relation : goal.getRelationsFromThisGoal()) {
			if (relation.getToGoal() != null && relation.getRelationType() != null) {
				edges.add(new Edge(relation.getRelationType().getLabel(), relation.getToGoal()));
				seen.add(relation.getRelationType().name() + ":" + relation.getToGoal().getId());
			}
		}
		for (GoalRelation relation : goal.getRelationsToThisGoal()) {
			Goal other = relation.getFromGoal();
			if (other == null || relation.getRelationType() == null) {
				continue;
			}
			if (relation.getRelationType().isSymmetric()
					&& seen.contains(relation.getRelationType().name() + ":" + other.getId())) {
				continue;
			}
			edges.add(new Edge(relation.getRelationType().getInverseLabel(), other));
		}
		edges.sort(Comparator.comparing(Edge::label).thenComparing(e -> e.goal(), BY_NAME));
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (Edge edge : edges) {
			candidates.add(() -> related(context, edge.goal(), edge.label(), RELATED_CHARS));
		}
		return fill(budget, candidates);
	}

	private record Edge(String label, Goal goal) {
	}
}
