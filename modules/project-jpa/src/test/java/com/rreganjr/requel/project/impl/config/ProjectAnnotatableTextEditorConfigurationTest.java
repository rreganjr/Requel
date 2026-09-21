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
package com.rreganjr.requel.project.impl.config;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

import com.rreganjr.requel.annotation.spi.DefaultAnnotatableTextEditRegistry;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectTeamImpl;
import com.rreganjr.requel.project.impl.ScenarioImpl;
import com.rreganjr.requel.project.impl.StepImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.UseCaseImpl;

/**
 * Parity between what the assistants analyse and what can be corrected (issue #305).
 * <p>
 * A lexical issue is raised by an assistant, and resolving its Fix Spelling position edits the
 * annotated entity through that entity's registered editor. So every type
 * {@code AssistantTaskRunner} can analyse needs a registration: adding an assistant for a new
 * entity type without one would leave that type's spelling issues unresolvable. This test is
 * the thing that fails when that happens.
 *
 * @author ron
 */
public class ProjectAnnotatableTextEditorConfigurationTest {

	private DefaultAnnotatableTextEditRegistry registry() throws Exception {
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		// The command factory is only touched when an editor builds a command, not when it is
		// registered, so registration needs no factory.
		InitializingBean initializer = new ProjectAnnotatableTextEditorConfiguration()
				.projectAnnotatableTextEditorInitializer(registry, null);
		initializer.afterPropertiesSet();
		return registry;
	}

	@Test
	public void registersAnEditorForEveryAnalysableEntityType() throws Exception {
		DefaultAnnotatableTextEditRegistry registry = registry();

		// Exactly the types AssistantTaskRunner analyses: goals, stories and actors directly,
		// use cases fanning out to scenarios and actors, and scenario steps.
		assertTrue(registry.resolveTextEditor(GoalImpl.class).isPresent(), "GoalImpl");
		assertTrue(registry.resolveTextEditor(StoryImpl.class).isPresent(), "StoryImpl");
		assertTrue(registry.resolveTextEditor(ActorImpl.class).isPresent(), "ActorImpl");
		assertTrue(registry.resolveTextEditor(UseCaseImpl.class).isPresent(), "UseCaseImpl");
		assertTrue(registry.resolveTextEditor(ScenarioImpl.class).isPresent(), "ScenarioImpl");
		assertTrue(registry.resolveTextEditor(StepImpl.class).isPresent(), "StepImpl");
	}

	@Test
	public void givesAScenarioItsOwnEditorRatherThanAStepOne() throws Exception {
		// ScenarioImpl extends StepImpl, so without its own registration the superclass walk
		// would hand a scenario EditScenarioStepCommand.
		DefaultAnnotatableTextEditRegistry registry = registry();

		assertNotSame(registry.resolveTextEditor(StepImpl.class).orElse(null),
				registry.resolveTextEditor(ScenarioImpl.class).orElse(null));
	}

	@Test
	public void leavesTypesNoAssistantAnalysesUnregistered() throws Exception {
		// Not an oversight: nothing annotates these, so a spelling issue cannot reach them.
		// If an assistant is ever taught to analyse one, this test is where the coupling
		// surfaces, alongside the parity test above.
		DefaultAnnotatableTextEditRegistry registry = registry();

		assertTrue(registry.resolveTextEditor(ProjectTeamImpl.class).isEmpty(), "ProjectTeamImpl");
		assertTrue(registry.resolveTextEditor(GlossaryTermImpl.class).isEmpty(), "GlossaryTermImpl");
	}
}
