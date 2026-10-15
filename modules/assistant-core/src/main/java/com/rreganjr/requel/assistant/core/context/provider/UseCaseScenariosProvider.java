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
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #261: a use case's primary scenario (marked) and its additional scenarios, each with its
 * steps in order; a step that is itself a scenario shows its steps, two levels deep.
 */
@Component
public class UseCaseScenariosProvider extends AbstractContextProvider {

	public static final String ID = "usecase-scenarios";
	static final int MAX_DEPTH = 2;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof UseCase;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		UseCase useCase = (UseCase) target;
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		Scenario primary = useCase.getScenario();
		if (primary != null) {
			candidates.add(() -> scenario(context, primary, "primary scenario", 1));
		}
		List<Scenario> additional = new ArrayList<>(useCase.getAdditionalScenarios());
		additional.sort(BY_NAME);
		for (Scenario scenario : additional) {
			if (primary != null && scenario.getId().equals(primary.getId())) {
				continue;
			}
			candidates.add(() -> scenario(context, scenario, "additional scenario", 1));
		}
		return fill(budget, candidates);
	}

	static RelatedEntity scenario(ProviderContext context, Scenario scenario, String relation,
			int depth) {
		List<RelatedEntity> steps = new ArrayList<>();
		List<Step> ordered = scenario.getSteps();
		for (int i = 0; i < ordered.size(); i++) {
			Step step = ordered.get(i);
			String stepRelation = "step " + (i + 1) + " of " + ordered.size();
			if (step instanceof Scenario nested && depth < MAX_DEPTH) {
				steps.add(scenario(context, nested, stepRelation + " (scenario)", depth + 1));
			} else {
				steps.add(related(context, step, stepRelation, RELATED_CHARS));
			}
		}
		return related(context, scenario, relation, RELATED_CHARS, steps);
	}
}
