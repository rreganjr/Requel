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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.UseCase;

class ProjectContextPackBuilderTest {

	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
			ZoneOffset.UTC);

	@Test
	void buildsPackFromPopulatedProject() {
		ProjectContextPackBuilder builder = newBuilder(new ContextPackSizeLimits());

		Project project = stubProject("Phoenix", "Reimagine the things",
				List.of(stubGoal(1L, "Reduce churn", "Churn reduction target")),
				List.of(stubActor(2L, "Customer", "End-user persona")),
				List.of(stubStory(3L, "Self-serve cancel", "Customer can cancel without calling",
						StoryType.Success, stubActor(2L, "Customer", "End-user persona"))),
				List.<UseCase>of(),
				List.<Scenario>of(),
				List.of(stubTerm(4L, "Churn", "The rate at which customers leave")));

		ProjectContextPack pack = builder.build(project);

		assertThat(pack.project().name()).isEqualTo("Phoenix");
		assertThat(pack.project().description()).isEqualTo("Reimagine the things");
		assertThat(pack.actors()).hasSize(1);
		assertThat(pack.actors().get(0).name()).isEqualTo("Customer");
		assertThat(pack.goals()).extracting(GoalSnapshot::name).containsExactly("Reduce churn");
		assertThat(pack.stories()).extracting(StorySnapshot::name)
				.containsExactly("Self-serve cancel");
		assertThat(pack.stories().get(0).storyType()).isEqualTo("Success");
		assertThat(pack.glossary().terms()).extracting(GlossaryTermSnapshot::name)
				.containsExactly("Churn");
		assertThat(pack.metadata().truncated()).isFalse();
		assertThat(pack.metadata().redactedFields()).isEmpty();
		assertThat(pack.metadata().totalCharacters()).isGreaterThan(0);
	}

	@Test
	void redactsTextFieldsThroughPolicy() {
		ContextPackSizeLimits limits = new ContextPackSizeLimits();
		RedactionPolicy policy = (field, value, notes) -> {
			if ("project.text".equals(field)) {
				notes.add("project.text redacted");
				return "[redacted]";
			}
			return value;
		};
		ProjectContextPackBuilder builder = new ProjectContextPackBuilder(policy, limits,
				fixedClock);
		Project project = stubProject("Phoenix", "Should not appear",
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

		ProjectContextPack pack = builder.build(project);

		assertThat(pack.project().description()).isEqualTo("[redacted]");
		assertThat(pack.metadata().redactedFields()).contains("project.text redacted");
	}

	@Test
	void clampsTextFieldsThatExceedPerFieldLimit() {
		ContextPackSizeLimits limits = new ContextPackSizeLimits();
		limits.setMaxTextCharsPerField(10);
		ProjectContextPackBuilder builder = newBuilder(limits);
		String longText = "abcdefghijklmnopqrstuvwxyz";
		Project project = stubProject("Phoenix", longText,
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

		ProjectContextPack pack = builder.build(project);

		assertThat(pack.project().description()).hasSize(10);
		assertThat(pack.metadata().truncated()).isTrue();
		assertThat(pack.metadata().truncationNotes())
				.anyMatch(note -> note.contains("project.text truncated"));
	}

	private ProjectContextPackBuilder newBuilder(ContextPackSizeLimits limits) {
		return new ProjectContextPackBuilder(new NoOpRedactionPolicy(), limits, fixedClock);
	}

	private static Project stubProject(String name, String text, List<Goal> goals,
			List<Actor> actors, List<Story> stories, List<UseCase> useCases,
			List<Scenario> scenarios, List<GlossaryTerm> terms) {
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(100L);
		when(project.getVersion()).thenReturn(1);
		when(project.getName()).thenReturn(name);
		when(project.getText()).thenReturn(text);
		when(project.getActors()).thenReturn(Set.copyOf(actors));
		when(project.getGoals()).thenReturn(Set.copyOf(goals));
		when(project.getStories()).thenReturn(Set.copyOf(stories));
		when(project.getUseCases()).thenReturn(Set.copyOf(useCases));
		when(project.getScenarios()).thenReturn(Set.copyOf(scenarios));
		// SortedSet for glossary
		java.util.TreeSet<GlossaryTerm> sortedTerms = new java.util.TreeSet<>(
				java.util.Comparator.comparing(GlossaryTerm::getName));
		sortedTerms.addAll(terms);
		when(project.getGlossaryTerms()).thenReturn(sortedTerms);
		when(project.getCreatedBy()).thenReturn(null);
		return project;
	}

	private static Goal stubGoal(long id, String name, String text) {
		Goal goal = mock(Goal.class);
		when(goal.getId()).thenReturn(id);
		when(goal.getVersion()).thenReturn(1);
		when(goal.getName()).thenReturn(name);
		when(goal.getText()).thenReturn(text);
		return goal;
	}

	private static Actor stubActor(long id, String name, String text) {
		Actor actor = mock(Actor.class);
		when(actor.getId()).thenReturn(id);
		when(actor.getVersion()).thenReturn(1);
		when(actor.getName()).thenReturn(name);
		when(actor.getText()).thenReturn(text);
		return actor;
	}

	private static Story stubStory(long id, String name, String text, StoryType type,
			Actor primaryActor) {
		Story story = mock(Story.class);
		when(story.getId()).thenReturn(id);
		when(story.getVersion()).thenReturn(1);
		when(story.getName()).thenReturn(name);
		when(story.getText()).thenReturn(text);
		when(story.getStoryType()).thenReturn(type);
		when(story.getPrimaryActor()).thenReturn(primaryActor);
		return story;
	}

	private static GlossaryTerm stubTerm(long id, String name, String text) {
		GlossaryTerm term = mock(GlossaryTerm.class);
		when(term.getId()).thenReturn(id);
		when(term.getVersion()).thenReturn(1);
		when(term.getName()).thenReturn(name);
		when(term.getText()).thenReturn(text);
		return term;
	}
}
