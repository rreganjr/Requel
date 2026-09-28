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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AnalyzeProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;

/**
 * Issue #268: a whole project is analyzed through the assistant SPI. An import, and the
 * {@code AnalyzeProject} command, queue one run per text entity (goals, stories, actors, use
 * cases, scenarios, steps and glossary terms) in one batch. In the test profile the executor is a
 * no-op, so the runs stay {@code QUEUED} and the tests read them back.
 */
public class ProjectAnalysisIT extends AbstractLexicalAssistantTest {

	private static final String PROJECT_XML = "xml/testProject.xml";

	private TransactionTemplate transactionTemplate;

	@Autowired
	void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Test
	public void importQueuesOneRunPerTextEntity() throws Exception {
		Project project = importProject(true);

		Set<String> expected = textEntities(project);
		List<String> queued = queuedTargets(project);
		assertFalse(expected.isEmpty(), "the sample project has text entities");
		assertEquals(expected.size(), queued.size(), "one run per entity, none twice: " + queued);
		assertEquals(expected, new HashSet<>(queued));
		assertTrue(queuedRuns(project).stream().allMatch(run -> run.getTaskType() == null),
				"import runs are ordinary analysis runs");
	}

	@Test
	public void importWithAnalysisOffQueuesNothing() throws Exception {
		Project project = importProject(false);

		assertEquals(List.of(), queuedTargets(project));
	}

	@Test
	public void analyzeProjectQueuesEveryEntityAgain() throws Exception {
		Project project = importProject(false);
		Set<String> expected = textEntities(project);

		AnalyzeProjectCommand command = getProjectCommandFactory().newAnalyzeProjectCommand();
		command.setEditedBy(projectUser());
		command.setProject(project);
		getCommandHandler().execute(command);

		List<String> queued = queuedTargets(project);
		assertEquals(expected.size(), queued.size(), "one run per entity: " + queued);
		assertEquals(expected, new HashSet<>(queued));
		assertTrue(queuedRuns(project).stream()
				.allMatch(run -> "project".equals(run.getTriggeredByUsername())),
				"the runs are on behalf of the user who asked");
	}

	// ---- fixtures -------------------------------------------------------------------------

	private Project importProject(boolean analysisEnabled) throws Exception {
		ensureDictionaryLoaded();
		User creator = projectUser();
		ImportProjectCommand command = getProjectCommandFactory().newImportProjectCommand();
		command.setAnalysisEnabled(analysisEnabled);
		command.setEditedBy(creator);
		command.setName("Analysis Project " + stamp());
		try (InputStream xml = getClass().getClassLoader().getResourceAsStream(PROJECT_XML)) {
			command.setInputStream(xml);
			return getCommandHandler().execute(command).getProject();
		}
	}

	private List<AssistantRunEntity> queuedRuns(Project project) {
		return assistantRunRepository.findAll().stream()
				.filter(run -> project.getId().equals(run.getProjectId())
						&& "QUEUED".equals(run.getStatus()))
				.collect(Collectors.toList());
	}

	private List<String> queuedTargets(Project project) {
		return queuedRuns(project).stream()
				.map(run -> run.getTargetType() + ":" + run.getTargetId())
				.collect(Collectors.toList());
	}

	/** "Type:id" for every entity whole-project analysis should cover, read from the model. */
	private Set<String> textEntities(Project project) {
		return transactionTemplate.execute(status -> {
			Project loaded = getProjectRepository().get(project);
			Set<String> keys = new HashSet<>();
			for (Goal goal : loaded.getGoals()) {
				keys.add("Goal:" + goal.getId());
			}
			for (Story story : loaded.getStories()) {
				keys.add("Story:" + story.getId());
			}
			for (Actor actor : loaded.getActors()) {
				keys.add("Actor:" + actor.getId());
			}
			for (UseCase useCase : loaded.getUseCases()) {
				keys.add("UseCase:" + useCase.getId());
			}
			List<Scenario> scenarios = new ArrayList<>(loaded.getScenarios());
			for (int i = 0; i < scenarios.size(); i++) {
				Scenario scenario = scenarios.get(i);
				keys.add("Scenario:" + scenario.getId());
				for (Step proxied : scenario.getSteps()) {
					Object step = Hibernate.unproxy(proxied);
					if (step instanceof Scenario nested) {
						if (!keys.contains("Scenario:" + nested.getId())) {
							scenarios.add(nested);
						}
					} else {
						keys.add("Step:" + proxied.getId());
					}
				}
			}
			for (GlossaryTerm term : loaded.getGlossaryTerms()) {
				keys.add("GlossaryTerm:" + term.getId());
			}
			return keys;
		});
	}
}
