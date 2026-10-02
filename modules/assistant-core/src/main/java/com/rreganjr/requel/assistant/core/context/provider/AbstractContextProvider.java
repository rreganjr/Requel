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
import java.util.List;
import java.util.function.Supplier;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.context.ContextProvider;
import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.TextEntity;

/**
 * Issue #261: shared plumbing for the built-in providers - entity refs, redacted names and texts,
 * and filling a section in priority order until the budget runs out.
 */
abstract class AbstractContextProvider implements ContextProvider {

	/** Text kept for a summary entry (a sibling goal, a referencing story). */
	static final int SUMMARY_CHARS = 300;
	/** Text kept for a closely related entry (a related goal, a scenario step). */
	static final int RELATED_CHARS = 1000;

	/** Orders entities by name, then id, so a section is the same on every run. */
	static final Comparator<ProjectOrDomainEntity> BY_NAME = Comparator
			.comparing((ProjectOrDomainEntity e) -> e.getName() == null ? "" : e.getName())
			.thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId());

	static EntityRef ref(ProjectOrDomainEntity entity) {
		return EntityRef.of(entity.getProjectOrDomainEntityInterface().getSimpleName(),
				entity.getId());
	}

	static RelatedEntity related(ProviderContext context, ProjectOrDomainEntity entity,
			String relation, int textChars, List<RelatedEntity> children) {
		String path = entity.getProjectOrDomainEntityInterface().getSimpleName() + "["
				+ entity.getId() + "]";
		return new RelatedEntity(ref(entity), relation,
				context.name(path + ".name", entity.getName()),
				context.text(path + ".text", textOf(entity), textChars), children);
	}

	/** The entity's own text: a {@link TextEntity}'s or a non-user stakeholder's; else none. */
	static String textOf(ProjectOrDomainEntity entity) {
		if (entity instanceof TextEntity text) {
			return text.getText();
		}
		if (entity instanceof NonUserStakeholder stakeholder) {
			return stakeholder.getText();
		}
		return null;
	}

	static RelatedEntity related(ProviderContext context, ProjectOrDomainEntity entity,
			String relation, int textChars) {
		return related(context, entity, relation, textChars, List.of());
	}

	/**
	 * Add {@code candidates} in order while they fit; the first that does not fit ends the
	 * section (so the cut is always the tail) and marks it truncated.
	 */
	ContextSection fill(ProviderBudget budget, List<Supplier<RelatedEntity>> candidates) {
		List<RelatedEntity> shown = new ArrayList<>();
		for (Supplier<RelatedEntity> candidate : candidates) {
			RelatedEntity entity = candidate.get();
			if (!budget.tryAdd(entity)) {
				return new ContextSection(id(), shown, shown.size(), candidates.size(), true);
			}
			shown.add(entity);
		}
		return new ContextSection(id(), shown, shown.size(), candidates.size(), false);
	}
}
