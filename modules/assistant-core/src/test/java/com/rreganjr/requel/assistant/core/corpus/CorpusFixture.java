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
package com.rreganjr.requel.assistant.core.corpus;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.TextEntity;
import com.rreganjr.requel.project.UseCase;

/** Issue #266: a mocked project for the corpus tests. Add entities, then call {@link #project}. */
final class CorpusFixture {

	final List<Goal> goals = new ArrayList<>();
	final List<Story> stories = new ArrayList<>();
	final List<Actor> actors = new ArrayList<>();
	final List<UseCase> useCases = new ArrayList<>();
	final List<Scenario> scenarios = new ArrayList<>();
	final List<GlossaryTerm> terms = new ArrayList<>();

	/** A mocked entity with an id, a name, a text and its domain interface. */
	static <T extends TextEntity> T entity(Class<T> type, Long id, String name, String text) {
		T entity = mock(type);
		when(entity.getId()).thenReturn(id);
		when(entity.getName()).thenReturn(name);
		when(entity.getText()).thenReturn(text);
		doReturn(type).when(entity).getProjectOrDomainEntityInterface();
		return entity;
	}

	Goal goal(long id, String name, String text) {
		Goal goal = entity(Goal.class, id, name, text);
		goals.add(goal);
		return goal;
	}

	Story story(long id, String name, String text) {
		Story story = entity(Story.class, id, name, text);
		stories.add(story);
		return story;
	}

	Actor actor(long id, String name) {
		Actor actor = entity(Actor.class, id, name, name + " uses the system");
		actors.add(actor);
		return actor;
	}

	UseCase useCase(long id, String name, String text) {
		UseCase useCase = entity(UseCase.class, id, name, text);
		useCases.add(useCase);
		return useCase;
	}

	Scenario scenario(long id, String name, Step... steps) {
		Scenario scenario = entity(Scenario.class, id, name, name);
		doReturn(List.of(steps)).when(scenario).getSteps();
		scenarios.add(scenario);
		return scenario;
	}

	GlossaryTerm term(Long id, String name) {
		GlossaryTerm term = entity(GlossaryTerm.class, id, name, "the meaning of " + name);
		terms.add(term);
		return term;
	}

	static Step step(long id, String text) {
		return entity(Step.class, id, text, text);
	}

	/** {@code from} relates to {@code to}, seen from both goals. */
	static void relate(Goal from, Goal to) {
		GoalRelation relation = mock(GoalRelation.class);
		when(relation.getFromGoal()).thenReturn(from);
		when(relation.getToGoal()).thenReturn(to);
		doReturn(new LinkedHashSet<>(List.of(relation))).when(from).getRelationsFromThisGoal();
		doReturn(new LinkedHashSet<>(List.of(relation))).when(to).getRelationsToThisGoal();
	}

	/** {@code goal}'s referers. */
	static void referers(Goal goal, Object... referers) {
		doReturn(new LinkedHashSet<>(List.of(referers))).when(goal).getReferers();
	}

	/** {@code term}'s referers. */
	static void referers(GlossaryTerm term, Object... referers) {
		doReturn(new LinkedHashSet<>(List.of(referers))).when(term).getReferers();
	}

	Project project(long id) {
		Project project = mock(Project.class);
		when(project.getId()).thenReturn(id);
		doReturn(new LinkedHashSet<>(goals)).when(project).getGoals();
		doReturn(new LinkedHashSet<>(stories)).when(project).getStories();
		doReturn(new LinkedHashSet<>(actors)).when(project).getActors();
		doReturn(new LinkedHashSet<>(useCases)).when(project).getUseCases();
		doReturn(new LinkedHashSet<>(scenarios)).when(project).getScenarios();
		TreeSet<GlossaryTerm> glossary = new TreeSet<>(Comparator.comparing(
				(GlossaryTerm term) -> term.getId() == null ? Long.MAX_VALUE : term.getId()));
		glossary.addAll(terms);
		doReturn(glossary).when(project).getGlossaryTerms();
		for (Goal goal : goals) {
			when(goal.getProjectOrDomain()).thenReturn(project);
		}
		for (UseCase useCase : useCases) {
			when(useCase.getProjectOrDomain()).thenReturn(project);
		}
		return project;
	}
}
