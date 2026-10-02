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
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Issue #263: the names of the project's actors and glossary terms, without their text, so a
 * review suggests an actor or term to create only when the project does not have it already.
 * Actors come first; each list is ordered by name.
 */
@Component
public class ProjectNamesProvider extends AbstractContextProvider {

	public static final String ID = "project-names";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof ProjectOrDomainEntity;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		ProjectOrDomainEntity entity = (ProjectOrDomainEntity) target;
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		ProjectOrDomain project = entity.getProjectOrDomain();
		if (project != null) {
			List<Actor> actors = new ArrayList<>();
			for (ProjectOrDomainEntity candidate : project.getProjectEntities()) {
				if (candidate instanceof Actor actor && !isTarget(entity, actor)) {
					actors.add(actor);
				}
			}
			actors.sort(BY_NAME);
			for (Actor actor : actors) {
				candidates.add(() -> nameOnly(context, actor, "project actor"));
			}
			List<GlossaryTerm> terms = new ArrayList<>(project.getGlossaryTerms());
			terms.removeIf(term -> isTarget(entity, term));
			terms.sort(BY_NAME);
			for (GlossaryTerm term : terms) {
				candidates.add(() -> nameOnly(context, term, "glossary term"));
			}
		}
		return fill(budget, candidates);
	}

	private static boolean isTarget(ProjectOrDomainEntity target, ProjectOrDomainEntity entity) {
		return target.getId() != null && target.getId().equals(entity.getId())
				&& target.getProjectOrDomainEntityInterface()
						.equals(entity.getProjectOrDomainEntityInterface());
	}

	private static RelatedEntity nameOnly(ProviderContext context, ProjectOrDomainEntity entity,
			String relation) {
		String path = entity.getProjectOrDomainEntityInterface().getSimpleName() + "["
				+ entity.getId() + "]";
		return new RelatedEntity(ref(entity), relation, context.name(path + ".name",
				entity.getName()), null);
	}
}
