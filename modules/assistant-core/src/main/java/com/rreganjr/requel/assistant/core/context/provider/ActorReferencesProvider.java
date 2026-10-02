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
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #261: the stories and use cases that reference an actor (summary), then the actor's own
 * goals. Stakeholders are deliberately absent: a person may fill an actor role, but that is not an
 * association the model can rely on.
 */
@Component
public class ActorReferencesProvider extends AbstractContextProvider {

	public static final String ID = "actor-references";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof Actor;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		Actor actor = (Actor) target;
		List<UseCase> useCases = new ArrayList<>();
		List<Story> stories = new ArrayList<>();
		for (ActorContainer referer : actor.getReferers()) {
			if (referer instanceof UseCase useCase) {
				useCases.add(useCase);
			} else if (referer instanceof Story story) {
				stories.add(story);
			}
		}
		useCases.sort(BY_NAME);
		stories.sort(BY_NAME);
		List<Goal> goals = new ArrayList<>(actor.getGoals());
		goals.sort(BY_NAME);
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (UseCase useCase : useCases) {
			String relation = useCase.getPrimaryActor() != null
					&& useCase.getPrimaryActor().getId().equals(actor.getId())
							? "primary actor of use case" : "actor in use case";
			candidates.add(() -> related(context, useCase, relation, SUMMARY_CHARS));
		}
		for (Story story : stories) {
			String relation = story.getPrimaryActor() != null
					&& story.getPrimaryActor().getId().equals(actor.getId())
							? "primary actor of story" : "actor in story";
			candidates.add(() -> related(context, story, relation, SUMMARY_CHARS));
		}
		for (Goal goal : goals) {
			candidates.add(() -> related(context, goal, "actor's goal", SUMMARY_CHARS));
		}
		return fill(budget, candidates);
	}
}
