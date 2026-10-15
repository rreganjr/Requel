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

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.core.context.ContextSection;
import com.rreganjr.requel.assistant.core.context.ProviderBudget;
import com.rreganjr.requel.assistant.core.context.ProviderContext;
import com.rreganjr.requel.assistant.core.context.RelatedEntity;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #261: where a step sits. For each scenario using it (up to {@value #MAX_SCENARIOS}): the
 * scenario, the steps either side of it, and the use cases using that scenario.
 */
@Component
public class StepSequenceProvider extends AbstractContextProvider {

	public static final String ID = "step-sequence";
	static final int MAX_SCENARIOS = 5;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof Step;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		Step step = (Step) target;
		List<Scenario> scenarios = new ArrayList<>(step.getUsingScenarios());
		scenarios.sort(Comparator.comparing(Scenario::getId));
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (Scenario scenario : scenarios.subList(0, Math.min(MAX_SCENARIOS, scenarios.size()))) {
			candidates.add(() -> placement(context, step, scenario));
		}
		ContextSection section = fill(budget, candidates);
		if (scenarios.size() <= MAX_SCENARIOS) {
			return section;
		}
		return new ContextSection(section.providerId(), section.entities(), section.shown(),
				scenarios.size(), true);
	}

	private static RelatedEntity placement(ProviderContext context, Step step,
			Scenario scenario) {
		List<Step> steps = scenario.getSteps();
		int at = -1;
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).getId().equals(step.getId())) {
				at = i;
				break;
			}
		}
		List<RelatedEntity> children = new ArrayList<>();
		if (at > 0) {
			children.add(related(context, steps.get(at - 1), "previous step", RELATED_CHARS));
		}
		if (at >= 0 && at < steps.size() - 1) {
			children.add(related(context, steps.get(at + 1), "next step", RELATED_CHARS));
		}
		List<UseCase> useCases = new ArrayList<>(scenario.getUsingUseCases());
		useCases.sort(BY_NAME);
		for (UseCase useCase : useCases) {
			children.add(related(context, useCase, "use case", SUMMARY_CHARS));
		}
		String relation = at >= 0 ? "step " + (at + 1) + " of " + steps.size() + " in scenario"
				: "used in scenario";
		return related(context, scenario, relation, SUMMARY_CHARS, children);
	}
}
