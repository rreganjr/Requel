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
package com.rreganjr.requel.assistant.core.context;

import java.util.List;
import java.util.Objects;

/**
 * Project-wide context for an assistant run. Carries enough metadata,
 * actor/goal/story/use-case summaries, scenarios, and glossary terms for the
 * assistant to reason about the project as a whole without traversing JPA
 * graphs.
 */
public record ProjectContextPack(ProjectSnapshot project, List<ActorSnapshot> actors,
		List<GoalSnapshot> goals, List<StorySnapshot> stories, List<UseCaseSnapshot> useCases,
		List<ScenarioSnapshot> scenarios, GlossarySnapshot glossary,
		ContextPackMetadata metadata) {

	public ProjectContextPack {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(metadata, "metadata");
		actors = actors == null ? List.of() : List.copyOf(actors);
		goals = goals == null ? List.of() : List.copyOf(goals);
		stories = stories == null ? List.of() : List.copyOf(stories);
		useCases = useCases == null ? List.of() : List.copyOf(useCases);
		scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
		if (glossary == null) {
			glossary = new GlossarySnapshot(List.of());
		}
	}
}
