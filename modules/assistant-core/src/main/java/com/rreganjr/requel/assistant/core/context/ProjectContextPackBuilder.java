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

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Builds {@link ProjectContextPack} instances from a loaded {@link Project}
 * domain entity. Pure function of its inputs: callers (the worker, an MCP
 * resource handler, etc.) load the project under their own transaction and
 * hand it in.
 *
 * <p>Per-field redaction runs through the injected {@link RedactionPolicy}
 * before clamping. Sizes are bounded by {@link ContextPackSizeLimits};
 * truncation and redaction notes flow into
 * {@link ContextPackMetadata}.</p>
 */
@Component
public class ProjectContextPackBuilder {

	private final RedactionPolicy redactionPolicy;
	private final ContextPackSizeLimits limits;
	private final Clock clock;

	public ProjectContextPackBuilder(RedactionPolicy redactionPolicy,
			ContextPackSizeLimits limits) {
		this(redactionPolicy, limits, Clock.systemUTC());
	}

	ProjectContextPackBuilder(RedactionPolicy redactionPolicy, ContextPackSizeLimits limits,
			Clock clock) {
		this.redactionPolicy = Objects.requireNonNull(redactionPolicy, "redactionPolicy");
		this.limits = Objects.requireNonNull(limits, "limits");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public ProjectContextPack build(Project project) {
		Objects.requireNonNull(project, "project");
		List<String> redacted = new ArrayList<>();
		List<String> truncated = new ArrayList<>();
		ContextPackBudget budget = new ContextPackBudget(limits.getMaxTotalCharacters());
		int maxField = limits.getMaxTextCharsPerField();

		String projectText = ContextPackTextUtils.prepareText("project.text", project.getText(),
				maxField, redactionPolicy, redacted, truncated);
		ProjectSnapshot projectSnapshot = new ProjectSnapshot(project.getId(), project.getVersion(),
				project.getName(), projectText,
				ContextPackTextUtils.username(project.getCreatedBy()));
		budget.add(projectSnapshot.name(), projectText);

		List<ActorSnapshot> actors = new ArrayList<>();
		for (Actor actor : project.getActors()) {
			if (budget.exceeded()) {
				truncated.add("actors list truncated by total-character budget");
				break;
			}
			String text = ContextPackTextUtils.prepareText("actor[" + actor.getId() + "].text",
					actor.getText(), maxField, redactionPolicy, redacted, truncated);
			actors.add(new ActorSnapshot(actor.getId(), actor.getVersion(), actor.getName(), text));
			budget.add(actor.getName(), text);
		}

		List<GoalSnapshot> goals = new ArrayList<>();
		for (Goal goal : project.getGoals()) {
			if (budget.exceeded()) {
				truncated.add("goals list truncated by total-character budget");
				break;
			}
			String text = ContextPackTextUtils.prepareText("goal[" + goal.getId() + "].text",
					goal.getText(), maxField, redactionPolicy, redacted, truncated);
			goals.add(new GoalSnapshot(goal.getId(), goal.getVersion(), goal.getName(), text));
			budget.add(goal.getName(), text);
		}

		List<StorySnapshot> stories = new ArrayList<>();
		for (Story story : project.getStories()) {
			if (budget.exceeded()) {
				truncated.add("stories list truncated by total-character budget");
				break;
			}
			String text = ContextPackTextUtils.prepareText("story[" + story.getId() + "].text",
					story.getText(), maxField, redactionPolicy, redacted, truncated);
			EntityRef primaryActor = story.getPrimaryActor() != null
					? EntityRef.of("Actor", story.getPrimaryActor().getId())
					: null;
			String storyTypeName = story.getStoryType() != null ? story.getStoryType().name() : null;
			stories.add(new StorySnapshot(story.getId(), story.getVersion(), story.getName(), text,
					storyTypeName, primaryActor));
			budget.add(story.getName(), text);
		}

		List<UseCaseSnapshot> useCases = new ArrayList<>();
		for (UseCase useCase : project.getUseCases()) {
			if (budget.exceeded()) {
				truncated.add("useCases list truncated by total-character budget");
				break;
			}
			String text = ContextPackTextUtils.prepareText("useCase[" + useCase.getId() + "].text",
					useCase.getText(), maxField, redactionPolicy, redacted, truncated);
			EntityRef primaryActor = useCase.getPrimaryActor() != null
					? EntityRef.of("Actor", useCase.getPrimaryActor().getId())
					: null;
			EntityRef primaryScenario = useCase.getScenario() != null
					? EntityRef.of("Scenario", useCase.getScenario().getId())
					: null;
			useCases.add(new UseCaseSnapshot(useCase.getId(), useCase.getVersion(), useCase.getName(),
					text, primaryActor, primaryScenario));
			budget.add(useCase.getName(), text);
		}

		List<ScenarioSnapshot> scenarios = new ArrayList<>();
		for (Scenario scenario : project.getScenarios()) {
			if (budget.exceeded()) {
				truncated.add("scenarios list truncated by total-character budget");
				break;
			}
			String scenarioText = ContextPackTextUtils.prepareText(
					"scenario[" + scenario.getId() + "].text", scenario.getText(), maxField,
					redactionPolicy, redacted, truncated);
			String scenarioTypeName = scenario.getType() != null
					? scenario.getType().name()
					: null;
			List<StepSnapshot> stepSnapshots = new ArrayList<>();
			for (Step step : scenario.getSteps()) {
				String stepText = ContextPackTextUtils.prepareText(
						"scenario[" + scenario.getId() + "].step[" + step.getId() + "].text",
						step.getText(), maxField, redactionPolicy, redacted, truncated);
				stepSnapshots.add(new StepSnapshot(step.getId(), step.getVersion(), step.getName(),
						stepText, step instanceof Scenario));
				budget.add(step.getName(), stepText);
			}
			scenarios.add(new ScenarioSnapshot(scenario.getId(), scenario.getVersion(),
					scenario.getName(), scenarioText, scenarioTypeName, stepSnapshots));
			budget.add(scenario.getName(), scenarioText);
		}

		List<GlossaryTermSnapshot> termSnapshots = new ArrayList<>();
		for (GlossaryTerm term : project.getGlossaryTerms()) {
			if (budget.exceeded()) {
				truncated.add("glossary list truncated by total-character budget");
				break;
			}
			String text = ContextPackTextUtils.prepareText("glossary[" + term.getId() + "].text",
					term.getText(), maxField, redactionPolicy, redacted, truncated);
			termSnapshots.add(new GlossaryTermSnapshot(term.getId(), term.getVersion(),
					term.getName(), text));
			budget.add(term.getName(), text);
		}
		GlossarySnapshot glossary = new GlossarySnapshot(termSnapshots);

		ContextPackMetadata metadata = new ContextPackMetadata(Instant.now(clock),
				budget.totalCharacters(), !truncated.isEmpty(), redacted, truncated);
		return new ProjectContextPack(projectSnapshot, actors, goals, stories, useCases, scenarios,
				glossary, metadata);
	}
}
