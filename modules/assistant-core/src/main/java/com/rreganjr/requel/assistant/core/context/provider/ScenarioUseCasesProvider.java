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
import com.rreganjr.requel.project.UseCase;

/** Issue #261: the use cases using a scenario, each with its primary actor. */
@Component
public class ScenarioUseCasesProvider extends AbstractContextProvider {

	public static final String ID = "scenario-usecases";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean appliesTo(Object target) {
		return target instanceof Scenario;
	}

	@Override
	public ContextSection contribute(Object target, ProviderBudget budget,
			ProviderContext context) {
		List<UseCase> useCases = new ArrayList<>(((Scenario) target).getUsingUseCases());
		useCases.sort(BY_NAME);
		List<Supplier<RelatedEntity>> candidates = new ArrayList<>();
		for (UseCase useCase : useCases) {
			candidates.add(() -> {
				List<RelatedEntity> children = useCase.getPrimaryActor() == null ? List.of()
						: List.of(related(context, useCase.getPrimaryActor(), "primary actor",
								SUMMARY_CHARS));
				return related(context, useCase, "uses this scenario", RELATED_CHARS, children);
			});
		}
		return fill(budget, candidates);
	}
}
