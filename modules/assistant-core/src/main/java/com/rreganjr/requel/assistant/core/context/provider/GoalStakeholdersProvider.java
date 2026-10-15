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
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;

/**
 * Issue #261: the stakeholders holding a goal, each with the other goals they hold, so a review
 * can check that one stakeholder's goals agree and do not clash with another's. A non-user
 * stakeholder (an organisation, a regulator) appears by name and description; a user stakeholder
 * by the run's role for that user ({@code user-N}, #262) and team only, never a person's name.
 */
@Component
public class GoalStakeholdersProvider extends AbstractContextProvider {

	public static final String ID = "goal-stakeholders";

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
		List<Stakeholder> holders = new ArrayList<>();
		for (GoalContainer container : goal.getReferers()) {
			if (container instanceof Stakeholder stakeholder) {
				holders.add(stakeholder);
			}
		}
		holders.sort(java.util.Comparator.comparing(Stakeholder::getId));
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (Stakeholder holder : holders) {
			candidates.add(() -> holder(context, goal, holder));
		}
		return fill(budget, candidates);
	}

	private static RelatedEntity holder(ProviderContext context, Goal goal, Stakeholder holder) {
		List<Goal> otherGoals = new ArrayList<>();
		for (Goal held : holder.getGoals()) {
			if (!held.getId().equals(goal.getId())) {
				otherGoals.add(held);
			}
		}
		otherGoals.sort(BY_NAME);
		List<RelatedEntity> children = new ArrayList<>();
		for (Goal held : otherGoals) {
			children.add(related(context, held, "also holds", SUMMARY_CHARS));
		}
		String path = "stakeholder[" + holder.getId() + "]";
		if (holder instanceof UserStakeholder user) {
			String role = context.author(user.getUser());
			String team = user.getTeam() == null ? null
					: context.name(path + ".team", user.getTeam().getName());
			return new RelatedEntity(ref(holder), "holds this goal",
					team == null ? role : role + " (team " + team + ")", null, children);
		}
		String text = holder instanceof NonUserStakeholder nonUser
				? context.text(path + ".text", nonUser.getText(), RELATED_CHARS)
				: null;
		return new RelatedEntity(ref(holder), "holds this goal",
				context.name(path + ".name", holder.getName()), text, children);
	}
}
