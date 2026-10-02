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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
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
		Set<Long> seen = new HashSet<>();
		for (ActorContainer referer : actor.getReferers()) {
			if (referer instanceof UseCase useCase && seen.add(useCase.getId())) {
				useCases.add(useCase);
			} else if (referer instanceof Story story && seen.add(story.getId())) {
				stories.add(story);
			}
		}
		// #263: a primary actor is its own link, not one of the container's actors, so an actor
		// that is only ever a primary actor has no referers and read as unused.
		ProjectOrDomain project = actor.getProjectOrDomain();
		if (project != null) {
			for (ProjectOrDomainEntity entity : project.getProjectEntities()) {
				if (entity instanceof UseCase useCase && isPrimary(actor, useCase.getPrimaryActor())
						&& seen.add(useCase.getId())) {
					useCases.add(useCase);
				} else if (entity instanceof Story story
						&& isPrimary(actor, story.getPrimaryActor()) && seen.add(story.getId())) {
					stories.add(story);
				}
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

	private static boolean isPrimary(Actor actor, Actor primary) {
		return primary != null && primary.getId() != null && primary.getId().equals(actor.getId());
	}
}
