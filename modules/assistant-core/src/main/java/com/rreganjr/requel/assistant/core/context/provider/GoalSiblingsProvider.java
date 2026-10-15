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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.assistant.core.context.TextSimilarity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Stakeholder;

/**
 * Issue #261: the project's other goals in summary form, so a review can see a goal that says the
 * same thing at another level of abstraction - the missing relation is the finding. Ranked so the
 * likely overlaps survive the budget: goals already related to this one, then goals a stakeholder
 * of this one also holds, then the rest by {@link TextSimilarity}; ties by name.
 */
@Component
public class GoalSiblingsProvider extends AbstractContextProvider {

	public static final String ID = "goal-siblings";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof Goal goal && goal.getProjectOrDomain() != null;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		Goal goal = (Goal) target;
		ProjectOrDomain project = goal.getProjectOrDomain();
		List<Goal> others = new ArrayList<>();
		for (Goal other : project.getGoals()) {
			if (!other.getId().equals(goal.getId())) {
				others.add(other);
			}
		}
		Set<Long> related = relatedIds(goal);
		Set<Long> sameStakeholder = sameStakeholderIds(goal);
		Map<Long, Double> score = similarity(goal, others, project);
		Comparator<Goal> order = Comparator
				.comparingInt((Goal g) -> related.contains(g.getId()) ? 0
						: sameStakeholder.contains(g.getId()) ? 1 : 2)
				.thenComparing(g -> -score.getOrDefault(g.getId(), 0.0))
				.thenComparing(g -> g, BY_NAME);
		others.sort(order);
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (Goal other : others) {
			String relation = related.contains(other.getId()) ? "related goal"
					: sameStakeholder.contains(other.getId()) ? "same stakeholder" : "sibling goal";
			candidates.add(() -> related(context, other, relation, SUMMARY_CHARS));
		}
		return fill(budget, candidates);
	}

	private static Set<Long> relatedIds(Goal goal) {
		Set<Long> ids = new HashSet<>();
		for (GoalRelation relation : goal.getRelationsFromThisGoal()) {
			if (relation.getToGoal() != null) {
				ids.add(relation.getToGoal().getId());
			}
		}
		for (GoalRelation relation : goal.getRelationsToThisGoal()) {
			if (relation.getFromGoal() != null) {
				ids.add(relation.getFromGoal().getId());
			}
		}
		return ids;
	}

	private static Set<Long> sameStakeholderIds(Goal goal) {
		Set<Long> ids = new HashSet<>();
		for (GoalContainer holder : goal.getReferers()) {
			if (holder instanceof Stakeholder) {
				for (Goal held : holder.getGoals()) {
					ids.add(held.getId());
				}
			}
		}
		return ids;
	}

	private static Map<Long, Double> similarity(Goal goal, List<Goal> others,
			ProjectOrDomain project) {
		List<String> texts = new ArrayList<>(others.size());
		for (Goal other : others) {
			texts.add(nameAndText(other));
		}
		double[] scores = new TextSimilarity(glossaryTokens(project)).scores(nameAndText(goal),
				texts);
		Map<Long, Double> byId = new HashMap<>();
		for (int i = 0; i < others.size(); i++) {
			byId.put(others.get(i).getId(), scores[i]);
		}
		return byId;
	}

	/** Each glossary term and its alternate names to the canonical term's name. */
	public static Map<String, String> glossaryTokens(ProjectOrDomain project) {
		Map<String, String> tokens = new HashMap<>();
		for (GlossaryTerm term : project.getGlossaryTerms()) {
			GlossaryTerm canonical = term.getCanonicalTerm() != null ? term.getCanonicalTerm()
					: term;
			if (term.getName() != null && canonical.getName() != null) {
				tokens.put(term.getName(), canonical.getName().toLowerCase());
			}
			for (GlossaryTerm alternate : term.getAlternateTerms()) {
				if (alternate.getName() != null && canonical.getName() != null) {
					tokens.put(alternate.getName(), canonical.getName().toLowerCase());
				}
			}
		}
		return tokens;
	}

	private static String nameAndText(Goal goal) {
		return (goal.getName() == null ? "" : goal.getName()) + " "
				+ (goal.getText() == null ? "" : goal.getText());
	}
}
